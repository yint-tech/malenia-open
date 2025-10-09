package cn.iinti.malenia2.service.proxy.client;

import cn.iinti.malenia2.api.ip.resource.IpResourceHelper;
import cn.iinti.malenia2.service.base.trace.Recorder;
import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Socks5 {
    private final DefaultChannelPromise promise;
    private final Channel channel;
    private final  ProxyInfo proxyInfo;
    private final Recorder recorder;
    private final URL url;

    public Socks5(DefaultChannelPromise promise, Channel channel,  ProxyInfo proxyInfo, Recorder recorder, URL url) {
        this.promise = promise;
        this.channel = channel;
        this.proxyInfo = proxyInfo;
        this.recorder = recorder;
        this.url = url;
    }


    void socks5handleShark() {
        channel.pipeline().addLast(new SocksMessageEncoder());
        List<SocksAuthScheme> socksAuthSchemes = Lists.newLinkedList();
        socksAuthSchemes.add(SocksAuthScheme.NO_AUTH);
        if (StringUtils.isNoneBlank(proxyInfo.getUserName(), proxyInfo.getPassword())) {
            socksAuthSchemes.add(SocksAuthScheme.AUTH_PASSWORD);
        }
        SocksInitRequest socksInitRequest = new SocksInitRequest(socksAuthSchemes);
        channel.writeAndFlush(socksInitRequest)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        promise.setFailure(future.cause());
                        return;
                    }
                    channel.pipeline().addFirst(new SocksInitResponseDecoder());
                    channel.pipeline().addLast(new SocksResponseHandler());
                });
    }

    private void handleUpstreamSocksResponse(SocksResponse socksResponse) {
        switch (socksResponse.responseType()) {
            case INIT:
                handleInitResponse((SocksInitResponse) socksResponse);
                break;
            case AUTH:
                handleAuthResponse((SocksAuthResponse) socksResponse);
                break;
            case CMD:
                handleCMDResponse((SocksCmdResponse) socksResponse);
                break;
            default:
                recorder.recordEvent(() -> "unknown socksResponse: " + socksResponse);
        }
    }

    private void handleInitResponse(SocksInitResponse response) {
        SocksAuthScheme socksAuthScheme = response.authScheme();
        switch (socksAuthScheme) {
            case NO_AUTH:
                doConnectToUpstream();
                break;
            case AUTH_PASSWORD:
                doAuth();
                break;
            default:
                promise.setFailure(new IOException("no support auth method: " + socksAuthScheme));
        }
    }

    private void doAuth() {
        recorder.recordEvent(() -> "send socks5 auth with user: ");
        channel.pipeline().addFirst(new SocksAuthResponseDecoder());
        SocksAuthRequest socksAuthRequest = new SocksAuthRequest(proxyInfo.getUserName(), proxyInfo.getPassword());
        channel.writeAndFlush(socksAuthRequest).addListener(future -> {
            if (!future.isSuccess()) {
                promise.tryFailure(future.cause());
                return;
            }
            recorder.recordEvent(() -> "auth request send finish");
        });
    }

    private void doConnectToUpstream() {
        SocksCmdRequest socksCmdRequest = new SocksCmdRequest(SocksCmdType.CONNECT,
                IpResourceHelper.isIpV4(url.getHost()) ? SocksAddressType.IPv4 : SocksAddressType.DOMAIN,
                url.getHost(), HttpClient.getPort(url));

        recorder.recordEvent(() -> "send cmd request to upstream");
        channel.pipeline().addFirst(new SocksCmdResponseDecoder());
        channel.writeAndFlush(socksCmdRequest)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        recorder.recordEvent(() -> "write upstream socksCmdRequest failed");
                        promise.setFailure(future.cause());
                    } else {
                        recorder.recordEvent(() -> "write upstream socksCmdRequest finished");
                        // now waiting cmd response
                    }
                });
    }

    private void handleAuthResponse(SocksAuthResponse response) {
        recorder.recordEvent(() -> "socks5 handShark authResponse: " + response.authStatus());
        if (response.authStatus() != SocksAuthStatus.SUCCESS) {
            promise.setFailure(new IOException("upstream auth failed"));
            return;
        }
        doConnectToUpstream();
    }

    private void handleCMDResponse(SocksCmdResponse response) {
        recorder.recordEvent(() -> "upstream CMD Response: " + response);
        if (response.cmdStatus() != SocksCmdStatus.SUCCESS) {
            recorder.recordEvent(() -> "cmd failed: " + response.cmdStatus());
            promise.setFailure(new IOException("cmd failed: " + response.cmdStatus()));
            return;
        }
        channel.pipeline().remove(SocksMessageEncoder.class);
        channel.pipeline().remove(SocksResponseHandler.class);
        recorder.recordEvent(() -> "upstream HandShark success finally");
        promise.trySuccess();
    }

    private class SocksResponseHandler extends SimpleChannelInboundHandler<SocksResponse> {


        @Override
        protected void channelRead0(ChannelHandlerContext ctx, SocksResponse msg) throws Exception {
            handleUpstreamSocksResponse(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            try {
                recorder.recordEvent("socks exception", cause);
                promise.tryFailure(cause);
            } finally {
                ReferenceCountUtil.release(cause);
            }
        }
    }
}
