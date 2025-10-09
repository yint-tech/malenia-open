package cn.iinti.malenia2.service.proxy.mock;

import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.IpAndPort;
import cn.iinti.malenia2.service.proxy.core.outbound.OutboundOperator;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import com.google.common.collect.Lists;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 在mock里面可以增加很详细的日志，因为他是调试环境
 */
public class ProxyMockHttpHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final Recorder recorder;

    private boolean isHttps = false;
    private final List<HttpObject> httpObjects = Lists.newLinkedList();

    public ProxyMockHttpHandler(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject httpObject) {
        if (httpObject.decoderResult().isFailure()) {
            recorder.recordEvent(() -> "Could not parse request from client. Decoder result: " + httpObject.decoderResult().toString());
            NettyUtil.httpResponseText(ctx.channel(), HttpResponseStatus.BAD_REQUEST,
                    "Unable to parse HTTP request");
            return;
        }
        recorder.recordEvent(() -> "receive http: " + httpObject);

        HttpRequest httpRequest;
        if (httpObject instanceof HttpRequest) {
            for (HttpObject obj : httpObjects) {
                ReferenceCountUtil.release(obj);
            }
            httpObjects.clear();
            httpRequest = (HttpRequest) httpObject;
        } else {
            httpObjects.add(httpObject);
            return;
        }
        if (isRequestToOriginServer(httpRequest)) {
            NettyUtil.httpResponseText(ctx.channel(), HttpResponseStatus.BAD_REQUEST, "this is proxy server,do not access server directly");
            return;
        }
        isHttps = isCONNECT(httpRequest);


        IpAndPort proxyTarget = NettyUtil.parseProxyTarget(httpRequest, isHttps);
        if (proxyTarget == null) {
            recorder.recordEvent(() -> "can not parse proxy target");
            NettyUtil.httpResponseText(ctx.channel(), HttpResponseStatus.BAD_REQUEST, "can not parse proxy target");
            ReferenceCountUtil.release(httpRequest);
            return;
        }

        if (!isHttps) {
            // http代理模式，需要暂停读，否则客户端可能一直发数据过来
            ctx.channel().config().setAutoRead(false);
        }
        Channel inboundChannel = ctx.channel();

        OutboundOperator.connectToServer(proxyTarget.getIp(), proxyTarget.getPort(), value -> {
            if (!value.isSuccess()) {
                recorder.recordEvent(() -> "connect to target failed", value.e);
                NettyUtil.httpResponseText(ctx.channel(), HttpResponseStatus.BAD_GATEWAY, "connect to upstream server failed");
                return;
            }
            Channel outboundChannel = value.v;
            NettyUtil.loveOther(inboundChannel, outboundChannel);
            if (isHttps) {
                onHttpsConnSuccess(httpRequest, inboundChannel, outboundChannel);
            } else {
                onHttpConnSuccess(httpRequest, inboundChannel, outboundChannel);
            }

        });
    }

    private void onHttpConnSuccess(HttpRequest httpRequest, Channel inboundChannel, Channel outboundChannel) {
        recorder.recordEvent(() -> "on http connect success");
        // http 模式
        inboundChannel.config().setAutoRead(true);
        ChannelPipeline outboundPipeline = outboundChannel.pipeline();
        outboundPipeline.addLast(new HttpClientCodec());

        ChannelPipeline inboundPipeline = inboundChannel.pipeline();

        NettyUtil.replay(inboundChannel, outboundChannel, recorder);

        inboundPipeline.remove(ProxyMockHttpHandler.class);
        outboundChannel.writeAndFlush(httpRequest);
    }

    private void onHttpsConnSuccess(HttpRequest httpRequest, Channel inboundChannel, Channel outboundChannel) {
        ReferenceCountUtil.release(httpRequest);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, CONNECTION_ESTABLISHED);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        NettyUtil.addVia(response, "virjar-spider-ha-proxy");
        recorder.recordEvent(() -> "https upstream connect success response a Connection established:" + response);
        inboundChannel.writeAndFlush(response)
                .addListener((ChannelFutureListener) channelFuture1 -> {
                    ChannelPipeline inboundPipeline = inboundChannel.pipeline();
                    inboundPipeline.remove(HttpServerCodec.class);

                    NettyUtil.replay(inboundChannel, outboundChannel, recorder);

                    inboundPipeline.remove(ProxyMockHttpHandler.class);

                });
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        for (HttpObject httpObject : httpObjects) {
            ctx.fireChannelRead(httpObject);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        recorder.recordEvent("error for embed http proxy sever", cause);
        ctx.close();
    }


    private boolean isRequestToOriginServer(HttpRequest httpRequest) {
        if (httpRequest.method() == HttpMethod.CONNECT) {
            return false;
        }
        // direct requests to the proxy have the path only without a scheme
        String uri = httpRequest.uri();
        return !HTTP_SCHEME.matcher(uri).matches();
    }


    private static final Pattern HTTP_SCHEME = Pattern.compile("^http://.*", Pattern.CASE_INSENSITIVE);

    private static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(
            200, "Connection established");

    public static boolean isCONNECT(HttpObject httpObject) {
        return httpObject instanceof HttpRequest
                && HttpMethod.CONNECT.equals(((HttpRequest) httpObject)
                .method());
    }
}
