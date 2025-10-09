package cn.iinti.malenia2.service.proxy.core.inbound.detector;


import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.inbound.handlers.ProxyHttpHandler;
import cn.iinti.malenia2.service.proxy.mock.ProxyMockHttpHandler;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.Set;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Matcher for plain http request.
 */
public abstract class HttpProxyMatcher extends ProtocolMatcher {


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
        if (!methods.contains(method) || firstURI == '/') {
            return MISMATCH;
        }

        return MATCH;
    }

    public static class HttpProxyUserIncome extends HttpProxyMatcher {

        @Override
        protected void handleMatched(Recorder recorder, ChannelHandlerContext ctx) {
            recorder.recordEvent("new mock http request ");
            ctx.pipeline().addLast(
                    new HttpServerCodec(),
                    new ProxyHttpHandler(recorder, false)
            );
        }
    }


    public static class HttpMockIncome extends HttpProxyMatcher {

        @Override
        protected void handleMatched(Recorder recorder, ChannelHandlerContext ctx) {
            recorder.recordEvent("new mock http request ");
            ctx.pipeline().addLast(
                    new HttpServerCodec(),
                    new ProxyMockHttpHandler(recorder)
            );
        }
    }


}
