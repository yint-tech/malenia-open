package cn.iinti.malenia2.service.proxy.client;

import cn.iinti.malenia2.service.base.trace.Recorder;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.net.URL;

public class Https {
    private final DefaultChannelPromise promise;
    private final Channel channel;
    private final ProxyInfo proxyInfo;
    private final Recorder recorder;
    private final URL url;
    private boolean hasSendAuthentication = false;

    public Https(DefaultChannelPromise promise, Channel channel, ProxyInfo proxyInfo, Recorder recorder, URL url) {
        this.promise = promise;
        this.channel = channel;
        this.proxyInfo = proxyInfo;
        this.recorder = recorder;
        this.url = url;
    }

    void handleShark() {
        channel.pipeline().addFirst(new HttpRequestEncoder());
        channel.pipeline().addFirst(new HttpResponseDecoder());

        DefaultFullHttpRequest connectRequest = createConnectRequest();
        channel.writeAndFlush(connectRequest)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        recorder.recordEvent(() -> "write https connect request failed");
                        promise.tryFailure(future.cause());
                        return;
                    }
                    recorder.recordEvent(() -> "request write finish ,setupResponseHandler");
                    channel.pipeline().addLast(new SimpleChannelInboundHandler<HttpResponse>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, HttpResponse msg) {
                            handleResponse(msg, this);
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                            // 发connect报文过程发现对方关闭了链接，这个时候可以重试
                            recorder.recordEvent(() -> "exception: ", cause);
                            promise.tryFailure(future.cause());
                        }
                    });
                });
    }

    private void handleResponse(HttpResponse httpResponse, SimpleChannelInboundHandler<HttpResponse> mHandler) {
        int code = httpResponse.status().code();
        recorder.recordEvent(() -> "ConnectHttpResponse:" + code);
        if (code == HttpResponseStatus.OK.code()) {
            recorder.recordEvent(() -> "hand shark success, remove upstream http netty handler");
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.remove(HttpRequestEncoder.class);
            pipeline.remove(HttpResponseDecoder.class);
            pipeline.remove(mHandler);
            promise.trySuccess();
            return;
        }

        if (code != HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED.code() && !hasSendAuthentication) {
            recorder.recordEvent(() -> "receive 5xx from upstream" + httpResponse);
            promise.tryFailure(new IOException(httpResponse.toString()));
            return;
        }


        // 有部分代理服务器首次发送了代理鉴权内容但是他不认，依然返回407
        // 然后有一些网络库一旦首次发送过密码，即使407也不会重新发送带鉴权请求，而是直接报告失败
        // 所以我们这里屏蔽这个问题，如果407我们再发送一次报文.
        recorder.recordEvent(() -> "407 response, send user pass again");
        DefaultFullHttpRequest connectRequest = createConnectRequest();

        channel.writeAndFlush(connectRequest)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        recorder.recordEvent(() -> "write https connect request failed");
                        promise.tryFailure(future.cause());
                        return;
                    }
                    recorder.recordEvent(() -> "second auth request write finish");
                    hasSendAuthentication = true;
                });
    }

    private DefaultFullHttpRequest createConnectRequest() {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.CONNECT, url.getHost());
        HttpClient.addProxyHeader(proxyInfo, request);
        request.headers().add(HttpHeaderNames.HOST, url.getHost());
        return request;
    }
}
