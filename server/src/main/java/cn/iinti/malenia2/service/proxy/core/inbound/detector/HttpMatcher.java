package cn.iinti.malenia2.service.proxy.core.inbound.detector;


import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.Session;
import cn.iinti.malenia2.service.proxy.core.inbound.handlers.NormalHttpPacketHandler;
import cn.iinti.malenia2.service.proxy.core.mitm.MitmHandler;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;

import java.util.Set;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Matcher for plain http request.
 */
public abstract class HttpMatcher extends ProtocolMatcher {


    private static final Set<String> methods = Sets.newHashSet("GET", "POST", "PUT", "HEAD", "OPTIONS", "PATCH", "DELETE",
            "TRACE");


    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 8) {
            return PENDING;
        }

        int index = buf.indexOf(0, 8, (byte) ' ');
        if (index < 0) {
            return MISMATCH;
        }

        int firstURIIndex = index + 1;
        if (buf.readableBytes() < firstURIIndex + 1) {
            return PENDING;
        }

        String method = buf.toString(0, index, US_ASCII);
        char firstURI = (char) (buf.getByte(firstURIIndex + buf.readerIndex()) & 0xff);
        if (!methods.contains(method) || firstURI != '/') {
            return MISMATCH;
        }

        return MATCH;
    }


    public static class HttpUserIncome extends HttpMatcher {

        @Override
        protected void handleMatched(Recorder recorder, ChannelHandlerContext ctx) {
            recorder.recordEvent("new normal http request ");
            ctx.pipeline().addLast(
                    new HttpServerCodec(),
                    new NormalHttpPacketHandler(recorder, false)
            );
        }
    }



    public static class HttpOverMitm extends HttpMatcher {
        private final Channel upstreamChannel;
        private final boolean isSSL;
        private final Session session;

        public HttpOverMitm(Channel upstreamChannel, Session session, boolean isSSL) {
            this.upstreamChannel = upstreamChannel;
            this.session = session;
            this.isSSL = isSSL;
        }

        @Override
        protected void handleMatched(Recorder recorder, ChannelHandlerContext ctx) {
            session.getRecorder().recordEvent(() -> "sniffer http ,aggregate http object and enter mitm");
            ChannelPipeline pipeline = ctx.pipeline();
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpServerExpectContinueHandler());
            pipeline.addLast(new HttpContentCompressor());
            pipeline.addLast(new HttpObjectAggregator(Settings.mitmAggregateSize.value));
            pipeline.addLast(new HttpContentDecompressor());
            pipeline.addLast(new MitmHandler(isSSL, session));

            ChannelPipeline outboundPipeline = upstreamChannel.pipeline();
            outboundPipeline.addLast(new HttpClientCodec());
            outboundPipeline.addLast(new HttpContentDecompressor());
            outboundPipeline.addLast(new HttpObjectAggregator(Settings.mitmAggregateSize.value));
            // todo 这里支持websocket和http2
            //outboundPipeline.addLast(new HttpUpgradeHandler());
            session.replay(upstreamChannel);
        }
    }
}
