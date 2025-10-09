package cn.iinti.malenia2.service.proxy.core.inbound.detector;


import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.ProxyServer;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;

import static cn.iinti.malenia2.utils.CommonUtils.UN_SUPPORT_PROTOCOL_MSG;

@Slf4j
public abstract class SSLMatcher extends ProtocolMatcher {
    protected final SslContext sslContext;

    public SSLMatcher(SslContext sslContext) {
        this.sslContext = sslContext;
        if (sslContext == null) {
            throw new IllegalStateException("no ssl context configured,please check you config");
        }
    }


    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 3) {
            return PENDING;
        }
        byte first = buf.getByte(buf.readerIndex());
        byte second = buf.getByte(buf.readerIndex() + 1);
        byte third = buf.getByte(buf.readerIndex() + 2);
        if (first == 22 && second <= 3 && third <= 3) {
            return MATCH;
        }
        return MISMATCH;
    }

    public static class ProxyIncome extends SSLMatcher {
        public ProxyIncome(SslContext sslContext) {
            super(sslContext);
        }

        @Override
        protected void handleMatched(Recorder recorder, ChannelHandlerContext ctx) {
            ctx.pipeline().addLast(
                    sslContext.newHandler(ctx.channel().alloc()),
                    ProxyServer.buildProtocolDetector(recorder)
            );
        }
    }

    public static class PortalIncome extends SSLMatcher {

        public PortalIncome(SslContext sslContext) {
            super(sslContext);
        }

        @Override
        protected void handleMatched(Recorder recorder, ChannelHandlerContext ctx) {
            ctx.pipeline().addLast(
                    sslContext.newHandler(ctx.channel().alloc()),
                    new ProtocolDetector(
                            recorder,
                            (ctx1, buf) -> {
                                Monitor.counter(BuildConfig.appName + ".portal.ssl.unknownProtocol").increment();
                                recorder.recordEvent("unsupported protocol:" + NettyUtil.formatByteBuf(ctx, "detect", buf));
                                buf.release();
                                ctx1.channel().writeAndFlush(Unpooled.wrappedBuffer(UN_SUPPORT_PROTOCOL_MSG))
                                        .addListener(ChannelFutureListener.CLOSE);
                            },
                            new HttpMatcher.HttpUserIncome())

            );
        }
    }

}
