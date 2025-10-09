package cn.iinti.malenia2.service.proxy.core.mitm.api.http;


import cn.iinti.malenia2.api.mitm.http.HttpVersion;

import javax.annotation.Nonnull;

public class HttpVersionImpl implements HttpVersion {
    final io.netty.handler.codec.http.HttpVersion nettyVersion;

    public HttpVersionImpl(io.netty.handler.codec.http.HttpVersion nettyVersion) {
        this.nettyVersion = nettyVersion;
    }

    @Override
    public String getProtocolName() {
        return nettyVersion.protocolName();
    }

    @Override
    public int getMajorVersion() {
        return nettyVersion.majorVersion();
    }

    @Override
    public int getMinorVersion() {
        return nettyVersion.minorVersion();
    }

    @Override
    public String getText() {
        return nettyVersion.text();
    }

    @Override
    public boolean isKeepAliveDefault() {
        return nettyVersion.isKeepAliveDefault();
    }

    @Override
    public int compareTo(@Nonnull HttpVersion o) {
        HttpVersionImpl httpVersion = (HttpVersionImpl) o;
        return nettyVersion.compareTo(httpVersion.nettyVersion);
    }
}