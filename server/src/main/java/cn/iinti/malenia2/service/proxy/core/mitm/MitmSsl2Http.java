package cn.iinti.malenia2.service.proxy.core.mitm;


import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.IpAndPort;
import cn.iinti.malenia2.service.proxy.core.Session;
import cn.iinti.malenia2.service.proxy.core.inbound.detector.HttpMatcher;
import cn.iinti.malenia2.service.proxy.core.inbound.detector.ProtocolDetector;
import cn.iinti.malenia2.service.proxy.core.ssl.ClientSSLContextManager;
import cn.iinti.malenia2.service.proxy.core.ssl.ServerSSLContextManager;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import java.util.ArrayDeque;
import java.util.Queue;

import static io.netty.handler.codec.ByteToMessageDecoder.MERGE_CUMULATOR;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_1_1;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_2;

/**
 * 解密ssl流量，如果ssl流量解密之后是http，则进入http的MITM流程
 */
public class MitmSsl2Http extends ChannelInboundHandlerAdapter {
    private final ByteToMessageDecoder.Cumulator cumulator = MERGE_CUMULATOR;
    private ByteBuf buf;
    private boolean isSSL;
    private Queue<ByteBuf> queue;
    private boolean removed;

    private final Session session;
    private final Recorder recorder;
    private final Channel upstreamChannel;

    public MitmSsl2Http(Session session, Channel upstreamChannel) {
        this.session = session;
        this.upstreamChannel = upstreamChannel;
        this.recorder = session.getRecorder();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf)) {
            recorder.recordEvent(() -> "unexpected message type for SSLDetector: " + msg.getClass());
            NettyUtil.closeOnFlush(ctx.channel());
            return;
        }

        ByteBuf in = (ByteBuf) msg;
        if (isSSL) {
            queue.add(in);
            return;
        }

        if (buf == null) {
            buf = in;
        } else {
            buf = cumulator.cumulate(ctx.alloc(), buf, in);
        }

        if (buf.readableBytes() < 3) {
            return;
        }
        byte first = buf.getByte(buf.readerIndex());
        byte second = buf.getByte(buf.readerIndex() + 1);
        byte third = buf.getByte(buf.readerIndex() + 2);
        if (!(first == 22 && second <= 3 && third <= 3)) {
            // not ssl
            sniffHttp1OrRaw(ctx, false);
            ctx.pipeline().remove(this);
            return;
        }

        isSSL = true;
        queue = new ArrayDeque<>(2);

        SslContext sslContext;
        if (session.getMitmInterceptor().getConfig().isDisableH2()) {
            recorder.recordEvent(() -> "user disable http2");
            sslContext = ClientSSLContextManager.getInstance().getNoneHttp2Context();
        } else {
            sslContext = ClientSSLContextManager.getInstance().getContext();
        }
        IpAndPort connectTarget = session.getConnectTarget();
        SslHandler sslHandler = sslContext.newHandler(ctx.alloc(), connectTarget.getIp(), connectTarget.getPort());
        ChannelPipeline outboundPipeline = upstreamChannel.pipeline();
        outboundPipeline.addLast(sslHandler);
        outboundPipeline.addLast(new ApplicationProtocolNegotiationHandler(HTTP_1_1) {
            @Override
            protected void configurePipeline(ChannelHandlerContext c, String protocol) {
                recorder.recordEvent(() -> "alpn with target server: " + connectTarget.getIp() + " : " + protocol);

                boolean useH2 = protocol.equalsIgnoreCase(HTTP_2);
                SslContext serverContext = ServerSSLContextManager.instance.createProxySSlContext(connectTarget.getIp(), useH2, recorder);
                SslHandler serverSSLHandler = serverContext.newHandler(ctx.alloc());
                ctx.pipeline().addLast(serverSSLHandler);
                ctx.pipeline().addLast(new ApplicationProtocolNegotiationHandler(HTTP_1_1) {
                    @Override
                    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
                        recorder.recordEvent(() -> "alpn with client: " + connectTarget.getIp() + ":" + protocol);
                        if (protocol.equalsIgnoreCase(HTTP_2)) {
                            recorder.recordEvent(() -> "this is http2");
                            sniffHttp2(ctx);
                        } else {
                            recorder.recordEvent(() -> "this is http1.x or raw");
                            sniffHttp1OrRaw(ctx, true);
                        }
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        if (NettyUtil.causedByClientClose(cause)) {
                            recorder.recordEvent(() -> "client closed connection", cause);
                        } else {
                            recorder.recordEvent(() -> "application protocol negotiation error", cause);
                        }
                        ctx.close();
                    }
                });
                if (!removed) {
                    ctx.pipeline().remove(MitmSsl2Http.this);
                    removed = true;
                }
            }
        });

    }

    private void sniffHttp2(ChannelHandlerContext ctx) {
        // todo
        recorder.recordEvent("not support http2 mitm now");
        // http2的协议，现在我们直接重放，不支持mitm
        session.replay(upstreamChannel);
    }


    private void sniffHttp1OrRaw(ChannelHandlerContext ctx, boolean isSSL) {
        ctx.pipeline().addLast(
                new ProtocolDetector(session.getRecorder(), (ctx1, buf) -> {
                    recorder.recordEvent(() -> "unknown protocol,replay direct");
                    session.replay(upstreamChannel);
                    ctx.pipeline().remove(ProtocolDetector.class);
                    ctx.fireChannelRead(buf);
                }, new HttpMatcher.HttpOverMitm(upstreamChannel, session, isSSL))
        );
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        recorder.recordEvent(() -> "ssl detector exception", cause);
        if (buf != null) {
            buf.release();
            buf = null;
        }
        if (queue != null) {
            ByteBuf b;
            while ((b = queue.poll()) != null) {
                b.release();
            }
        }
        NettyUtil.closeOnFlush(ctx.channel());
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        removed = true;
        if (buf != null) {
            ctx.fireChannelRead(buf);
        }
        if (queue != null) {
            ByteBuf b;
            while ((b = queue.poll()) != null) {
                ctx.fireChannelRead(b);
            }
        }
        ctx.flush();
        queue = null;
        buf = null;
    }
}
