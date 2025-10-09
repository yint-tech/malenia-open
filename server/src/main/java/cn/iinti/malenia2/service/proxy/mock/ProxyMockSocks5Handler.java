package cn.iinti.malenia2.service.proxy.mock;


import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.outbound.OutboundOperator;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import io.netty.channel.*;
import io.netty.handler.codec.socks.*;
import io.netty.util.NetUtil;

public class ProxyMockSocks5Handler extends SimpleChannelInboundHandler<SocksRequest> {

    private final Recorder recorder;

    public ProxyMockSocks5Handler(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksRequest socksRequest) {
        recorder.recordEvent(() -> "mock inbound handle socks request：" + socksRequest.requestType());
        switch (socksRequest.requestType()) {
            case INIT:
                ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                ctx.writeAndFlush(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                break;
            case AUTH:
                ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                ctx.writeAndFlush(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                break;
            case CMD:
                handleCmd(ctx, socksRequest);
                break;
            case UNKNOWN:
                ctx.close();
                break;
            default:
        }
    }


    private void handleCmd(ChannelHandlerContext ctx, SocksRequest socksRequest) {
        SocksCmdRequest req = (SocksCmdRequest) socksRequest;
        if (req.cmdType() != SocksCmdType.CONNECT) {
            recorder.recordEvent(() -> "not support s5 cmd: " + req.cmdType());
            ctx.close();
            return;
        }

        OutboundOperator.connectToServer(req.host(), req.port(), value -> {
            if (!value.isSuccess()) {
                recorder.recordEvent(() -> "connect to upstream failed", value.e);
                ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, req.addressType()))
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }

            Channel inbound = ctx.channel();
            Channel outbound = value.v;

            NettyUtil.loveOther(inbound, outbound);

            ctx.channel().writeAndFlush(
                    new SocksCmdResponse(SocksCmdStatus.SUCCESS,
                            NetUtil.isValidIpV4Address(req.host()) ? SocksAddressType.IPv4 : SocksAddressType.DOMAIN,
                            req.host(), req.port())
            ).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    recorder.recordEvent(() -> "socket closed when write socks success", future.cause());
                    return;
                }
                ChannelPipeline pipeline = ctx.pipeline();
                pipeline.remove(SocksMessageEncoder.class);
                pipeline.remove(ProxyMockSocks5Handler.class);

                NettyUtil.replay(inbound, outbound, recorder);
            });

        });

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        recorder.recordEvent(() -> "Socks5InboundHandler handler error", cause);
        ctx.close();
    }
}