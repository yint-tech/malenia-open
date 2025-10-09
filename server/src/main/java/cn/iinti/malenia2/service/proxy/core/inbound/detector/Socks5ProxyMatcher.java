package cn.iinti.malenia2.service.proxy.core.inbound.detector;


import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.inbound.handlers.ProxySocks5Handler;
import cn.iinti.malenia2.service.proxy.mock.ProxyMockSocks5Handler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksMessageEncoder;

/**
 * Matcher for socks5 proxy protocol
 */
public abstract class Socks5ProxyMatcher extends ProtocolMatcher {


    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            return PENDING;
        }
        byte first = buf.getByte(buf.readerIndex());
        byte second = buf.getByte(buf.readerIndex() + 1);
        if (first == 5) {
            return MATCH;
        }
        return MISMATCH;
    }


    public static class Socks5UserIncome extends Socks5ProxyMatcher {
        @Override
        protected void handleMatched(Recorder recorder,ChannelHandlerContext ctx) {
            ctx.pipeline().addLast(
                    new SocksInitRequestDecoder(),
                    new SocksMessageEncoder(),
                    new ProxySocks5Handler()
            );
        }
    }

    public static class Socks5MockIncome extends Socks5ProxyMatcher {
        @Override
        protected void handleMatched(Recorder recorder, ChannelHandlerContext ctx) {
            ctx.pipeline().addLast(
                    new SocksInitRequestDecoder(),
                    new SocksMessageEncoder(),
                    new ProxyMockSocks5Handler(recorder)
            );
        }
    }


}
