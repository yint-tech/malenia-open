package cn.iinti.malenia2.service.proxy.core.mitm.api.http;


import cn.iinti.malenia2.api.mitm.http.HttpHeaders;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpHeadersImpl implements HttpHeaders {
    private final io.netty.handler.codec.http.HttpHeaders nettyHeaders;

    public HttpHeadersImpl(io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
        this.nettyHeaders = nettyHeaders;
    }

    @Override
    public String get(String name) {
        return nettyHeaders.get(name);
    }

    @Override
    public String get(String name, String defaultValue) {
        return nettyHeaders.get(name, defaultValue);
    }

    @Override
    public Integer getInt(String name) {
        return nettyHeaders.getInt(name);
    }

    @Override
    public Integer getInt(String name, int defaultValue) {
        return nettyHeaders.getInt(name, defaultValue);
    }

    @Override
    public Long getTimeMillis(String name) {
        return nettyHeaders.getTimeMillis(name);
    }

    @Override
    public Long getTimeMillis(String name, long defaultValue) {
        return nettyHeaders.getTimeMillis(name, defaultValue);
    }

    @Override
    public List<String> getAll(String name) {
        return nettyHeaders.getAll(name);
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return nettyHeaders.iteratorAsString();
    }

    @Override
    public boolean contains(String name) {
        return nettyHeaders.contains(name);
    }

    @Override
    public boolean isEmpty() {
        return nettyHeaders.isEmpty();
    }

    @Override
    public int size() {
        return nettyHeaders.size();
    }

    @Override
    public Set<String> names() {
        return nettyHeaders.names();
    }

    @Override
    public HttpHeaders add(String name, Object value) {
        nettyHeaders.add(name, value);
        return this;
    }

    @Override
    public HttpHeaders add(String name, Iterable<?> values) {
        nettyHeaders.add(name, values);
        return this;
    }

    @Override
    public HttpHeaders add(HttpHeaders headers) {
        HttpHeadersImpl i = (HttpHeadersImpl) headers;
        nettyHeaders.add(i.nettyHeaders);
        return this;
    }

    @Override
    public HttpHeaders addInt(String name, int value) {
        nettyHeaders.addInt(name, value);
        return this;
    }

    @Override
    public HttpHeaders set(String name, Object value) {
        nettyHeaders.set(name, value);
        return this;
    }

    @Override
    public HttpHeaders set(String name, Iterable<?> values) {
        nettyHeaders.set(name, values);
        return this;
    }

    @Override
    public HttpHeaders set(HttpHeaders headers) {
        HttpHeadersImpl i = (HttpHeadersImpl) headers;
        nettyHeaders.set(i.nettyHeaders);
        return this;
    }

    @Override
    public HttpHeaders remove(String name) {
        nettyHeaders.remove(name);
        return this;
    }

    @Override
    public HttpHeaders clear() {
        nettyHeaders.clear();
        return this;
    }

    @Override
    public boolean contains(String name, String value, boolean ignoreCase) {
        return nettyHeaders.contains(name, value, ignoreCase);
    }

    @Override
    public boolean containsValue(String name, String value, boolean ignoreCase) {
        return nettyHeaders.containsValue(name, value, ignoreCase);
    }

    @Override
    public String getAsString(String name) {
        return nettyHeaders.getAsString(name);
    }

    @Override
    public List<String> getAllAsString(String name) {
        return nettyHeaders.getAllAsString(name);
    }

    @Override
    public HttpHeaders copy() {
        io.netty.handler.codec.http.HttpHeaders copy = nettyHeaders.copy();
        return new HttpHeadersImpl(copy);
    }
}