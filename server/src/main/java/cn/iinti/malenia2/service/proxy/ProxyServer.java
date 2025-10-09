package cn.iinti.malenia2.service.proxy;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.Session;
import cn.iinti.malenia2.service.proxy.core.inbound.detector.*;
import cn.iinti.malenia2.service.proxy.core.inbound.handlers.ProxyHttpHandler;
import cn.iinti.malenia2.service.proxy.core.inbound.handlers.ProxySocks5Handler;
import cn.iinti.malenia2.service.proxy.core.ssl.KeyStoreLoader;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperProduct;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import static cn.iinti.malenia2.utils.CommonUtils.UN_SUPPORT_PROTOCOL_MSG;


/**
 * 描述一个代理服务器
 */
@Slf4j
public class ProxyServer {

    @Getter
    public int port;


    private Channel serverChannel;

    @Getter
    private final WrapperProduct wrapperProduct;

    public ProxyServer(int port, WrapperProduct wrapperProduct) {
        this.port = port;
        this.wrapperProduct = wrapperProduct;
        startProxy(buildProxyServerConfig(), 20);
    }

    private void startProxy(ServerBootstrap serverBootstrap, int leftRetry) {
        if (leftRetry < 0) {
            log.error("the proxy server start failed finally!!:{}", port);
            return;
        }
        serverBootstrap.bind(port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("proxy server start success: {}", port);
                serverChannel = future.channel();
                return;
            }
            log.info("proxy server start failed, will be retry after 5s", future.cause());
            future.channel().eventLoop().schedule(() -> startProxy(
                    serverBootstrap, leftRetry - 1), 5, TimeUnit.SECONDS
            );
        });
    }

    public boolean enable() {
        return serverChannel != null && serverChannel.isActive();
    }


    private ServerBootstrap buildProxyServerConfig() {
        return new ServerBootstrap()
                .group(NettyThreadPools.proxyServerBossGroup, NettyThreadPools.proxyServerWorkerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        Monitor.counter(BuildConfig.appName + ".session.touch").increment();

                        Session session = Session.touch(ch, ProxyServer.this);
                        ch.pipeline().addLast(buildProtocolDetector(session.getRecorder()));

                        setupProtocolTimeoutCheck(ch, session.getRecorder());
                    }
                });
    }

    private static void setupProtocolTimeoutCheck(Channel channel, Recorder recorder) {
        WeakReference<Channel> ref = new WeakReference<>(channel);
        EventLoop eventLoop = channel.eventLoop();

        eventLoop.schedule(() ->
                        doHandlerTimeoutCheck(ref, ProtocolDetector.class, recorder, "protocol detect timeout, close this channel"),
                5, TimeUnit.SECONDS);

        eventLoop.schedule(() -> {
            doHandlerTimeoutCheck(ref, ProxyHttpHandler.class, recorder, "http proxy init timeout");
            doHandlerTimeoutCheck(ref, ProxySocks5Handler.class, recorder, "socks5 proxy init timeout");
        }, 2, TimeUnit.MINUTES);

    }

    private static <T extends ChannelHandler> void doHandlerTimeoutCheck(WeakReference<Channel> ref, Class<T> handlerClazz, Recorder recorder, String msgHit) {
        Channel ch = ref.get();
        if (ch == null) {
            return;
        }
        T handler = ch.pipeline().get(handlerClazz);
        if (handler != null) {
            recorder.recordEvent(msgHit);
            ch.close();
        }
    }

    public static ProtocolDetector buildProtocolDetector(Recorder recorder) {
        return new ProtocolDetector(
                recorder,
                (ctx, buf) -> {
                    Monitor.counter(BuildConfig.appName + ".session.unknownProtocol").increment();
                    recorder.recordEvent("unsupported protocol:" + NettyUtil.formatByteBuf(ctx, "detect", buf));
                    buf.release();
                    ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(UN_SUPPORT_PROTOCOL_MSG))
                            .addListener(ChannelFutureListener.CLOSE);
                },
                new HttpMatcher.HttpUserIncome(),
                new HttpProxyMatcher.HttpProxyUserIncome(),
                new HttpsProxyMatcher.HttpsProxyUserIncome(),
                new Socks5ProxyMatcher.Socks5UserIncome(),
                // ssl的方式访问malenia代理（指： 业务到malenia的链接是https的，和到真实目标网站，如https://www.baidu.com 不是一个概念）
                new SSLMatcher.ProxyIncome(KeyStoreLoader.chooseServerSSLContext(recorder)
                )
        );
    }

    public void destroy() {
        NettyUtil.closeIfActive(serverChannel);
    }
}
