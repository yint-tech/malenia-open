package cn.iinti.malenia2.service.proxy.core.mitm.api.http;

import cn.iinti.malenia2.api.mitm.data.Content;
import cn.iinti.malenia2.api.mitm.http.HttpHeaders;
import cn.iinti.malenia2.api.mitm.http.HttpMessage;
import cn.iinti.malenia2.api.mitm.http.HttpVersion;
import cn.iinti.malenia2.service.proxy.core.mitm.api.ApiImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 实现dsl中定义的http的抽象和netty的桥接器
 */
public class HttpMessageImpl implements HttpMessage {
    @Nonnull
    private FullHttpMessage fullHttpMessage;

    public HttpMessageImpl(@Nonnull FullHttpMessage fullHttpMessage) {
        this.fullHttpMessage = fullHttpMessage;
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return new HttpVersionImpl(fullHttpMessage.protocolVersion());
    }

    @Override
    public HttpMessage setProtocolVersion(HttpVersion version) {
        HttpVersionImpl httpVersion = (HttpVersionImpl) version;
        fullHttpMessage.setProtocolVersion(httpVersion.nettyVersion);
        return this;
    }

    @Override
    public HttpHeaders getHeaders() {
        return new HttpHeadersImpl(fullHttpMessage.headers());
    }

    private byte[] consumedBytes = null;

    private Content content;

    @Override
    public byte[] getBytes() {
        // 请注意这里只能读一次，所以我们需要把他捞到缓存中
        if (consumedBytes != null) {
            return consumedBytes;
        }
        ByteBuf content = fullHttpMessage.content();
        if (content == null) {
            return null;
        }
        consumedBytes = new byte[content.capacity()];
        content.readBytes(consumedBytes);
        return consumedBytes;
    }

    @Override
    public HttpMessage setBytes(byte[] newContent) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(newContent);
        FullHttpMessage drop = fullHttpMessage;
        fullHttpMessage = drop.replace(byteBuf);
        ReferenceCountUtil.release(drop);

        if (content != null) {
            content.setModel(null);
        }
        consumedBytes = newContent;
        return this;
    }

    @Override
    public String getUrl() {
        if (this instanceof HttpResponseImpl) {
            HttpResponseImpl httpResponse = (HttpResponseImpl) this;
            return httpResponse.getHttpRequest().getUrl();
        }
        if (!(this instanceof HttpRequestImpl)) {
            throw new IllegalStateException("error http message type: " + getClass());
        }


        HttpRequestImpl httpRequest = (HttpRequestImpl) this;

        try {
            URI uri = new URI(httpRequest.getUri());
            if (StringUtils.isNotBlank(uri.getHost())) {
                return httpRequest.getUri();
            }
        } catch (URISyntaxException e) {
            ApiImpl.static_log("uri error", e);
        }

        String host = getHeaders().get("Host");
        if (StringUtils.isBlank(host)) {
            ApiImpl.static_log("can not get host header");
            throw new IllegalStateException("can not get host header");
        }

        return (httpRequest.isHttps() ? "https" : "http") + "://" +
                host +
                httpRequest.getUri();
    }

    @Override
    public Content getContent() {
        if (content == null) {
            content = new Content(this);
        }
        return content;
    }

    public void flushContent() {
        if (content == null) {
            return;
        }
        byte[] data = content.serialize();
        setBytes(data);
    }

}
