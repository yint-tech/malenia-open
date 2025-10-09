package cn.iinti.malenia2.service.proxy.client;

import cn.iinti.malenia2.service.base.safethread.NoShakingValueCallback;
import cn.iinti.malenia2.service.base.safethread.ValueCallback;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.NettyThreadPools;
import cn.iinti.malenia2.service.proxy.core.outbound.OutboundOperator;
import cn.iinti.malenia2.service.proxy.core.ssl.SSLContextException;
import com.google.common.io.BaseEncoding;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class HttpClient {

    private static final SslContext sslContext = buildSSLContext();

    private static SslContext buildSSLContext() {
        try {
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE);
            if (OpenSsl.isAvailable()) {
                sslContextBuilder.sslProvider(SslProvider.OPENSSL);
            } else {
                sslContextBuilder.sslProvider(SslProvider.JDK);
            }
            return sslContextBuilder.build();
        } catch (SSLException e) {
            throw new SSLContextException(e);
        }
    }

    public static void openHttp(String url, ProxyInfo proxyInfo, Recorder recorder,
                                ValueCallback<Channel> callback) {
        URL urlModel;
        try {
            urlModel = new URL(url);
        } catch (Exception e) {
            ValueCallback.failed(callback, e);
            return;
        }
        URL finalUrlModel = urlModel;
        recorder.recordEvent(() -> "open connection to " + url);
        ChannelFuture future = connect(urlModel, proxyInfo, recorder);
        future.addListener((ChannelFutureListener) channelFuture -> {
            if (!channelFuture.isSuccess()) {
                recorder.recordEvent(() -> "failed to connect to " + url, future.cause());
                ValueCallback.failed(callback, channelFuture.cause());
                return;
            }
            recorder.recordEvent(() -> "Got upstream socket channel");
            Channel channel = channelFuture.channel();

            boolean isHttps = finalUrlModel.getProtocol().equals("https");
            if (isHttps) {
                recorder.recordEvent(() -> "add ssl handle");
                SslHandler sslHandler = sslContext.newHandler(channel.alloc(), finalUrlModel.getHost(), finalUrlModel.getPort());
                channel.pipeline().addLast(sslHandler);
            }
            channel.pipeline().addLast(new HttpClientCodec());
            //io.netty.handler.codec.http.TooLongHttpContentException: Response entity too large: DefaultHttpResponse(decodeResult: success, version: HTTP/1.1)
            //Date: Sun, 01 Dec 2024 16:57:54 GMT
            //Content-Length: 8242
            channel.pipeline().addLast(new HttpObjectAggregator(4 << 20));
            ValueCallback.success(callback, channel);
        });
    }


    private static ChannelFuture connect(URL url, ProxyInfo proxyInfo, Recorder recorder) {
        ChannelFuture channelFuture;

        if (proxyInfo != null && proxyInfo.getProxyType() != Proxy.Type.DIRECT) {
            channelFuture = proxyConnect(proxyInfo, url, recorder);
        } else {
            channelFuture = createClientBootstrap().connect(url.getHost(), getPort(url));
        }
        return channelFuture;
    }

    private static Bootstrap createClientBootstrap() {
        return new Bootstrap()
                .group(NettyThreadPools.asyncHttpWorkGroup)
                .channelFactory(NioSocketChannel::new)
                .handler(new OutboundOperator.StubHandler())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);
    }

    private static ChannelFuture proxyConnect(ProxyInfo proxyInfo, URL url, Recorder recorder) {
        ChannelFuture connect = createClientBootstrap().connect(proxyInfo.getHost(), proxyInfo.getPort());
        DefaultChannelPromise defaultChannelPromise = new DefaultChannelPromise(connect.channel());
        connect.addListener((ChannelFutureListener) channelFuture -> {
            if (!channelFuture.isSuccess()) {
                defaultChannelPromise.setFailure(channelFuture.cause());
                return;
            }
            Channel lowLevelChannel = channelFuture.channel();

            // 这里比较粗暴的在10分钟后关闭强行关闭链接，避免泄漏
            WeakReference<Channel> ref = new WeakReference<>(lowLevelChannel);
            WeakReference<DefaultChannelPromise> promiseRef = new WeakReference<>(defaultChannelPromise);
            lowLevelChannel.eventLoop().schedule(() -> {
                Channel channel = ref.get();
                if (channel != null) {
                    channel.close().addListener((ChannelFutureListener) future -> {
                                DefaultChannelPromise promise = promiseRef.get();
                                if (promise != null) {
                                    promise.tryFailure(new IOException("upstream timeout"));
                                }
                            }

                    );
                }
            }, 10, TimeUnit.MINUTES);

            if (proxyInfo.getProxyType() == Proxy.Type.SOCKS) {
                new Socks5(defaultChannelPromise, lowLevelChannel, proxyInfo, recorder, url)
                        .socks5handleShark();
                return;
            }
            boolean isHttps = url.getProtocol().equals("https");
            if (isHttps) {
                new Https(defaultChannelPromise, lowLevelChannel, proxyInfo, recorder, url).handleShark();
            } else {
                defaultChannelPromise.trySuccess();
            }
        });
        return defaultChannelPromise;
    }

    static void execute(String url, Recorder recorder, ProxyInfo proxyInfo, ValueCallback<String> callback,
                        Supplier<FullHttpRequest> requestSupplier) {
        NoShakingValueCallback<String> noShakingValueCallback = new NoShakingValueCallback<>(callback);
        openHttp(url, proxyInfo, recorder, value -> {
            if (!value.isSuccess()) {
                ValueCallback.failed(noShakingValueCallback, value.e);
                return;
            }
            Channel v = value.v;
            ChannelPipeline pipeline = v.pipeline();
            // close connection if no response after 10 minute
            pipeline.addFirst(new IdleStateHandler(0, 0, 600));
            pipeline.addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
                    Charset charset = HttpUtil.getCharset(msg);
                    String response = msg.content().toString(charset);
                    try {
                        ValueCallback.success(noShakingValueCallback, response);
                    } finally {
                        v.close();
                    }
                }

                @Override
                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                    if (evt instanceof IdleStateEvent) {
                        onClose("read timeout,close connection");
                        return;
                    }
                    super.userEventTriggered(ctx, evt);
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    onClose("exception caught");
                    recorder.recordEvent(() -> "exception caught " + url, cause);
                }

                private void onClose(String msg) {
                    recorder.recordEvent(() -> msg);
                    try {
                        ValueCallback.failed(noShakingValueCallback, msg);
                    } finally {
                        v.close();
                    }
                }

            });

            FullHttpRequest defaultFullHttpRequest = requestSupplier.get();
            recorder.recordEvent(() -> "request http request: " + defaultFullHttpRequest);
            v.writeAndFlush(defaultFullHttpRequest).addListener((ChannelFutureListener) channelFuture -> {
                if (!channelFuture.isSuccess()) {
                    recorder.recordEvent(() -> "write http request failed: ", channelFuture.cause());
                    ValueCallback.failed(noShakingValueCallback, channelFuture.cause());
                }
            });
        });
    }


    static DefaultFullHttpRequest createRequest(HttpMethod method, URL url, ProxyInfo proxyInfo) {
        boolean usePlainHttpProxy = proxyInfo != null && proxyInfo.getProxyType() == Proxy.Type.HTTP &&
                "http".equals(url.getProtocol());
        DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                method, !usePlainHttpProxy ? toURI(url.toString()) : url.toString());
        if (usePlainHttpProxy && StringUtils.isNoneBlank(proxyInfo.getUserName(), proxyInfo.getPassword())) {
            addProxyHeader(proxyInfo, defaultFullHttpRequest);
        }
        defaultFullHttpRequest.headers().add(HttpHeaderNames.HOST, url.getHost());
        return defaultFullHttpRequest;
    }

    @SneakyThrows
    public static URL parseURL(String url) {
        return new URL(url);
    }

    public static String toURI(String uri) {
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            // truncate plain proxy config,this become normal http request
            int splitStartIndex = uri.indexOf("//");
            int index = uri.indexOf('/', splitStartIndex + 2);
            if (index > 0) {
                uri = uri.substring(index);
            } else {
                uri = "/";
            }
            return uri;
        }
        return uri;
    }

    public static int getPort(URL url) {
        int port = url.getPort();
        if (port <= 0 || port >= 65535) {
            return "https".equals(url.getProtocol()) ? 443 : 80;
        }
        return port;
    }

    public static void addProxyHeader(ProxyInfo proxyInfo, HttpRequest httpRequest) {
        String authorizationBody = proxyInfo.getUserName() + ":" + proxyInfo.getPassword();
        String httpAuthenticationHeader = "Basic " + BaseEncoding.base64().encode(authorizationBody.getBytes(StandardCharsets.UTF_8));
        httpRequest.headers().add(HttpHeaderNames.PROXY_AUTHORIZATION, httpAuthenticationHeader);
    }
}
