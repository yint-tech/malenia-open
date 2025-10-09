package cn.iinti.malenia2.service.proxy.core.inbound.handlers;


import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.IpAndPort;
import cn.iinti.malenia2.service.proxy.core.Session;
import cn.iinti.malenia2.service.proxy.core.mitm.MitmHandler;
import cn.iinti.malenia2.service.proxy.core.mitm.MitmSsl2Http;
import cn.iinti.malenia2.service.proxy.core.outbound.ActiveProxyIp;
import cn.iinti.malenia2.service.proxy.core.outbound.handshark.Protocol;
import cn.iinti.malenia2.service.proxy.core.switcher.OutboundConnectTask;
import cn.iinti.malenia2.service.proxy.core.switcher.UpstreamHandSharkCallback;
import cn.iinti.malenia2.service.proxy.dbconfigs.DbConfigs;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperOrder;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import com.google.common.io.BaseEncoding;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Queue;

public class ProxyHttpHandler extends AbstractFirstHttpHandler
        implements UpstreamHandSharkCallback {
    private static final byte[] authenticationRequiredData = """
            <!DOCTYPE HTML "-//IETF//DTD HTML 2.0//EN">
            <html>
                <head><title>407 Proxy Authentication Required</title></head>
                <body>
                    <h1>Proxy Authentication Required</h1>
                    <p>This server could not verify that you
                       are authorized to access the document
                       requested.  Either you supplied the wrong
                       credentials (e.g., bad password), or your
                       browser doesn't understand how to supply
                       the credentials required.
                    </p>
                </body>
            </html>
            """.getBytes(StandardCharsets.UTF_8);
    private static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(200,
            "Connection established");

    private Session session;

    public ProxyHttpHandler(Recorder recorder, boolean isHttps) {
        super(recorder, isHttps);
    }


    @Override
    void afterInitRequest() {
        session = Session.get(ctx.channel());
        if (!prepare()) {
            return;
        }
        // 暂停读
        ctx.channel().config().setAutoRead(false);
        OutboundConnectTask.startConnectOutbound(session, this);
    }

    private boolean prepare() {
        if (!session.isAuthed()) {
            Pair<String, String> userPwd = extractUserPwd(httpRequest);
            // remove PROXY_AUTHORIZATION after extract
            httpRequest.headers().remove(HttpHeaderNames.PROXY_AUTHORIZATION);

            DbConfigs.doAuth(session, userPwd.getLeft(), userPwd.getRight());
        }

        if (!session.isAuthed()) {
            recorder.recordEvent("auth failed, write 407");
            writeAuthenticationRequired();
            return false;
        }

        WrapperOrder wrapperOrder = session.getWrapperOrder();
        if (wrapperOrder == null) {
            recorder.recordEvent(() -> "blocking access because of none purchase");
            NettyUtil.httpResponseText(ctx.channel(), HttpResponseStatus.PAYMENT_REQUIRED,
                    "your account :" + session.getWrapperUser().getUserName()
                            + " must purchase this product :" + session.getProxyServer().getWrapperProduct().getProductId()
                            + " before your access proxy service"
            );
            return false;
        }

        if (session.getWrapperUser().getBalance() <= 0) {
            recorder.recordEvent(() -> "blocking access because of no user balance left");
            NettyUtil.httpResponseText(ctx.channel(), HttpResponseStatus.PAYMENT_REQUIRED,
                    "your account no balance left"
            );
            return false;
        }


        IpAndPort proxyTarget = NettyUtil.parseProxyTarget(httpRequest, isHttps);
        if (proxyTarget == null) {
            recorder.recordEvent(() -> "can not parse proxy target");
            NettyUtil.httpResponseText(ctx.channel(), HttpResponseStatus.BAD_REQUEST, "can not parse proxy target");
            return false;
        }
        session.onProxyTargetResolved(proxyTarget, isHttps ? Protocol.HTTPS : Protocol.HTTP);
        return true;
    }


    private Pair<String, String> extractUserPwd(HttpRequest httpRequest) {
        List<String> values = httpRequest.headers().getAll(
                HttpHeaderNames.PROXY_AUTHORIZATION);
        if (values.isEmpty()) {
            return Pair.of(null, null);
        }
        String fullValue = values.iterator().next();

        String value = StringUtils.substringAfter(fullValue, "Basic ").trim();

        if (value.equals("*")) {
            //  Proxy-Authorization: Basic *
            //  被扫描，有客户端发过来的是*
            return Pair.of(null, null);
        }

        try {
            byte[] decodedValue = BaseEncoding.base64().decode(value);
            String decodedString = new String(decodedValue, StandardCharsets.UTF_8);
            if (!decodedString.contains(":") || decodedString.trim().equals(":")) {
                //  Proxy-Authorization: Basic *
                //  被扫描，有客户端发过来的是 *
                //  Proxy-Authorization: Basic Og==
                //  Og== <----> :
                return Pair.of(null, null);
            }

            String userName = StringUtils.substringBefore(decodedString, ":");
            String password = StringUtils.substringAfter(decodedString, ":");
            return Pair.of(userName, password);

        } catch (Exception e) {
            recorder.recordEvent(() -> "auth error for  " + HttpHeaderNames.PROXY_AUTHORIZATION + ":" + value, e);
            ctx.close();
            // recorder.recordEvent(TAG, "auth error for: " + e.getClass().getName() + ":" + e.getMessage());
            return Pair.of(null, null);
        }
    }

    private void writeAuthenticationRequired() {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED,
                Unpooled.copiedBuffer(authenticationRequiredData)
        );
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, authenticationRequiredData.length);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
        response.headers().set(HttpHeaderNames.DATE, new Date());
        response.headers().set("Proxy-Authenticate",
                "Basic realm=\"Basic\"");
        NettyUtil.sendHttpResponse(ctx, httpRequest, response);
    }

    @Override
    public void onHandSharkFinished(Channel upstreamChannel, @Nullable Protocol outboundProtocol) {
        NettyUtil.loveOther(session.getInboundChannel(), upstreamChannel);

        ctx.channel().eventLoop().execute(() -> {
            if (!session.getInboundChannel().isActive()) {
                recorder.recordEvent("session closed after handShark finished");
                return;
            }

            ChannelFuture future = isHttps ? onHttpsHandShark(upstreamChannel) :
                    onHttpHandleShark(upstreamChannel, outboundProtocol);
            future.addListener((ChannelFutureListener) setupFuture -> {
                if (!setupFuture.isSuccess()) {
                    return;
                }
                afterWriteResponse();
            });
        });

    }

    private void afterWriteResponse() {
        Channel channel = ctx.channel();
        channel.eventLoop().execute(() -> {
            if (!channel.isActive()) {
                return;
            }
            // 需要切换一下线程，否则：
            // [2024-03-08 11:07:35,281 [outbound-oOOooOoOOo-4-6]  WARN cn.iinti.malenia2.0O.OooOoOOooOoOOo.oOOoOoOo:581]  An exception was thrown by cn.iinti.malenia2.0O.OoOoOoOo.oOoOOo$$Lambda$1274/0x00000008409d6440.operationComplete()
            // java.util.NoSuchElementException: cn.iinti.malenia2.0O.OoOoOoOo.oOoOOo
            // at cn.iinti.malenia2.0O.OoOoOoOo.oOoOOo.oO(ProxyHttpHandler.java:223)
            channel.config().setAutoRead(true);
            ctx.pipeline().remove(ProxyHttpHandler.class);
        });
    }

    private void attachUpstreamAuth(Channel upstreamChannel, Protocol outboundProtocol, HttpRequest httpRequest) {
        if (outboundProtocol != Protocol.HTTP) {
            // http代理，但是上游是直连、socks5等其他协议，此时不再需要增加鉴权，否则鉴权明文会被穿透
            recorder.recordEvent("outbound not use http, not need authentication");
            return;
        }

        String httpAuthenticationHeader = ActiveProxyIp.getBinding(upstreamChannel)
                .buildHttpAuthenticationInfo(session.getSessionParam(), recorder);
        if (httpAuthenticationHeader == null) {
            recorder.recordEvent(() -> "this ipSource do not need authentication");
            return;
        }
        httpRequest.headers().add(HttpHeaderNames.PROXY_AUTHORIZATION, httpAuthenticationHeader);
        recorder.recordEvent(() -> "fill authorizationContent: " + httpAuthenticationHeader);

    }

    private ChannelFuture onHttpHandleShark(Channel upstreamChannel, @Nullable Protocol outboundProtocol) {
        // http 会马上回掉到这里来
        ChannelPipeline outboundPipeline = upstreamChannel.pipeline();
        outboundPipeline.addLast(new HttpClientCodec());

        if (session.getMitmInterceptor() == null) {
            // 没有脚本的时候在这里填充上游密码，有脚本的时候不能在这里填充密码
            // 因为脚本可以访问这个结构体，所以有脚本的时候，我们在需要在脚本执行之后再刷新鉴权的header
            attachUpstreamAuth(upstreamChannel, outboundProtocol, httpRequest);
            recorder.recordEvent(() -> "start http tuning");
            session.replay(upstreamChannel);
            return upstreamChannel.newSucceededFuture();
        }
        // 有脚本的情况
        // 有mitm的情况下，需要上游http聚合成http,并且还要对他进行解压，因为解压之后才能处理content
        outboundPipeline.addLast(new HttpObjectAggregator(Settings.mitmAggregateSize.value));
        outboundPipeline.addLast(new HttpContentDecompressor());

        recorder.recordEvent("this http message need call interceptor");
        ChannelPipeline inboundPipeline = ctx.pipeline();
        inboundPipeline.addLast(new HttpContentCompressor());
        inboundPipeline.addLast(new HttpObjectAggregator(Settings.mitmAggregateSize.value));
        inboundPipeline.addLast(new HttpContentDecompressor());
        inboundPipeline.addLast(new MitmHandler(false, session));
        inboundPipeline.addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
                if (msg instanceof HttpRequest) {
                    attachUpstreamAuth(upstreamChannel, outboundProtocol, (HttpRequest) msg);
                }
                super.channelRead(ctx, msg);
            }
        });

        session.replay(upstreamChannel);
        return upstreamChannel.newSucceededFuture();
    }

    private ChannelFuture onHttpsHandShark(Channel upstreamChannel) {
        recorder.recordEvent(() -> "do https forward");
        DefaultFullHttpResponse connectEstablishResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, CONNECTION_ESTABLISHED);
        connectEstablishResponse.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        NettyUtil.addVia(connectEstablishResponse, "virjar-spider-ha-proxy");
        Queue<HttpObject> httpObjects = this.httpObjects;
        if (httpObjects != null) {
            httpObjects.clear();
        }

        ChannelPromise channelPromise = upstreamChannel.newPromise();

        session.getInboundChannel()
                .writeAndFlush(connectEstablishResponse)
                .addListener(future -> {
                    try {
                        releaseHttpObjects();
                        if (!future.isSuccess()) {
                            channelPromise.tryFailure(future.cause());
                            recorder.recordEvent(() -> "socket closed before write connect success message");
                            return;
                        }
                        ChannelPipeline inboundPipeline = session.getInboundChannel().pipeline();
                        inboundPipeline.remove(HttpServerCodec.class);
                        if (session.noneMITM()) {
                            recorder.recordEvent(() -> "start https replay tuning");
                            session.replay(upstreamChannel);
                        } else {
                            recorder.recordEvent(() -> "has mitm config,start decode ssl data");
                            // https协议状态下，存在拦截器，那么需要解密ssl流量，暂时不能执行重放
                            inboundPipeline.addLast(new MitmSsl2Http(session, upstreamChannel));
                        }

                        channelPromise.trySuccess();
                    } catch (Throwable e) {
                        recorder.recordEvent(() -> "setup onHttpsHandShark error", e);
                        ctx.fireExceptionCaught(e);
                    }
                });

        return channelPromise;
    }


    @Override
    public void onHandSharkError(Throwable e) {
        recorder.recordEvent(() -> "onHandSharkError error", e);
        NettyUtil.httpResponseText(ctx.channel(),
                HttpResponseStatus.BAD_GATEWAY,
                "http proxy system error\n " + NettyUtil.throwableMsg(e));
    }
}
