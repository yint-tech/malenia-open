package cn.iinti.malenia2.service.proxy.core.inbound.detector;


import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.inbound.handlers.ProxyHttpHandler;
import cn.iinti.malenia2.service.proxy.mock.ProxyMockHttpHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerCodec;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Matcher for http proxy connect tunnel.
 */
public class HttpsProxyMatcher extends ProtocolMatcher {


    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 8) {
            return PENDING;
        }

        String method = buf.toString(0, 8, US_ASCII);
        if (!"CONNECT ".equalsIgnoreCase(method)) {
            return MISMATCH;
        }

        return MATCH;
    }


    public static class HttpsProxyUserIncome extends HttpsProxyMatcher {
        @Override
        protected void handleMatched(Recorder recorder, ChannelHandlerContext ctx) {
            ctx.pipeline().addLast(
                    new HttpServerCodec(),
                    new ProxyHttpHandler(recorder, true)
            );
        }
    }

    public static class HttpsProxyMockIncome extends HttpsProxyMatcher {
        @Override
        protected void handleMatched(Recorder recorder, ChannelHandlerContext ctx) {
            ctx.pipeline().addLast(
                    new HttpServerCodec(),
                    new ProxyMockHttpHandler(recorder)
            );
        }
    }

}
