package cn.iinti.malenia2.service.proxy;

import com.google.common.base.Suppliers;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.function.Supplier;

public class NettyThreadPools {

    public static final NioEventLoopGroup internalWebBossGroup = new NioEventLoopGroup(1, createThreadFactory("internal-web-boss"));
    public static final NioEventLoopGroup internalWebWorkerGroup = new NioEventLoopGroup(1, createThreadFactory("internal-web-worker"));


    public static final NioEventLoopGroup outboundGroup = newDefaultEventLoop("outbound");

    public static final NioEventLoopGroup bandwidthLimitWorkGroup = newDefaultEventLoop("bandwidth-limit");
    public static final NioEventLoopGroup proxyServerBossGroup = newDefaultEventLoop("Proxy-boss-group");
    public static final NioEventLoopGroup proxyServerWorkerGroup = newDefaultEventLoop("Proxy-worker-group");

    // 下载代理，通知外部等业务http调用
    public static final NioEventLoopGroup asyncHttpWorkGroup = newDefaultEventLoop("async-http-invoker");

    // mock代理模块在生产不启动，他只是给我们用户快速体验提供的一种机制
    private static final Supplier<NioEventLoopGroup> mockProxyServerBoosGroup =
            Suppliers.memoize(() -> new NioEventLoopGroup(1, createThreadFactory("mock-proxy-boss")));
    private static final Supplier<NioEventLoopGroup> mockProxyServerWorkerGroup =
            Suppliers.memoize(() -> new NioEventLoopGroup(1, createThreadFactory("mock-proxy-worker")));

    public static NioEventLoopGroup getMockProxyServerBossGroup() {
        return mockProxyServerBoosGroup.get();
    }

    public static NioEventLoopGroup getMockProxyServerWorkerGroup() {
        return mockProxyServerWorkerGroup.get();
    }

    private static NioEventLoopGroup newDefaultEventLoop(String name) {
        return new NioEventLoopGroup(0, createThreadFactory(name));
    }

    private static DefaultThreadFactory createThreadFactory(String name) {
        return new DefaultThreadFactory(name + "-" + DefaultThreadFactory.toPoolName(NioEventLoopGroup.class));
    }
}
