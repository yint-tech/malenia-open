package cn.iinti.malenia2.service.proxy.mock;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.base.trace.impl.SubscribeRecorders;
import cn.iinti.malenia2.service.proxy.NettyThreadPools;
import cn.iinti.malenia2.service.proxy.core.inbound.detector.HttpProxyMatcher;
import cn.iinti.malenia2.service.proxy.core.inbound.detector.HttpsProxyMatcher;
import cn.iinti.malenia2.service.proxy.core.inbound.detector.ProtocolDetector;
import cn.iinti.malenia2.service.proxy.core.inbound.detector.Socks5ProxyMatcher;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import cn.iinti.malenia2.service.proxy.utils.PortSpaceParser;
import com.google.common.collect.Sets;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.TreeSet;

/**
 * 启动一个嵌入式代理服务资源池，主要用于测试，生产环境应该关闭这个服务。
 * <br/>
 * 本服务模拟上游代理商提供的代理服务，完成代理链路的模拟。
 * 他只有代理能力，且所有代理ip是唯一的出口ip是服务器本身
 * 他没有鉴权
 */
@Slf4j
public class MockProxyServer {
    private static TreeSet<Integer> mockServerPort = new TreeSet<>();

    public static Set<Integer> getRunningMockPorts() {
        return Sets.newHashSet(mockServerPort);
    }

    public static void startMockService() {
        if (StringUtils.isBlank(Environment.mockProxySpace)) {
            log.info("not need start mock proxy server");
            return;
        }
        if (!mockServerPort.isEmpty()) {
            log.warn("mock proxy server has been startup already!!");
            return;
        }

        String space = Environment.mockProxySpace;
        log.info("begin start mock proxy server from port: {}", space);
        mockServerPort = PortSpaceParser.parsePortSpace(space);

        ServerBootstrap httpProxyBootstrap = createMockServerBootstrap();
        for (Integer port : mockServerPort) {
            log.info("start mock http proxy server:{}", port);
            httpProxyBootstrap.bind("127.0.0.1", port).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("mock proxy server start success:{}", port);
                } else {
                    log.error("mock proxy server start failed", future.cause());
                }
            });
        }

    }


    private static ServerBootstrap createMockServerBootstrap() {
        return new ServerBootstrap()
                .group(NettyThreadPools.getMockProxyServerBossGroup(),
                        NettyThreadPools.getMockProxyServerWorkerGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(@NotNull SocketChannel ch) {
                        Recorder recorder = SubscribeRecorders.MOCK_SESSION.acquireRecorder(Environment.isLocalDebug);

                        ch.pipeline().addLast(
                                new ProtocolDetector(
                                        recorder,
                                        (ctx, buf) -> {
                                            Monitor.counter(BuildConfig.appName + ".session.unknownProtocol").increment();
                                            recorder.recordEvent("unsupported protocol:" + NettyUtil.formatByteBuf(ctx, "detect", buf));
                                            buf.release();
                                        },
                                        new Socks5ProxyMatcher.Socks5MockIncome(),
                                        new HttpProxyMatcher.HttpMockIncome(),
                                        new HttpsProxyMatcher.HttpsProxyMockIncome()
                                )
                        );
                    }
                });
    }
}
