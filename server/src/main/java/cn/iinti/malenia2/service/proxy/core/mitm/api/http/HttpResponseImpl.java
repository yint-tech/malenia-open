package cn.iinti.malenia2.service.proxy.core.mitm.api.http;

import cn.iinti.malenia2.api.mitm.http.HttpRequest;
import cn.iinti.malenia2.api.mitm.http.HttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;

public class HttpResponseImpl extends HttpMessageImpl implements HttpResponse {
    @Setter
    @Getter
    private FullHttpResponse fullHttpResponse;
    private final HttpRequestImpl request;

    public HttpResponseImpl(FullHttpResponse fullHttpResponse, HttpRequestImpl request) {
        super(fullHttpResponse);
        this.fullHttpResponse = fullHttpResponse;
        this.request = request;
    }

    @Override
    public HttpRequest getHttpRequest() {
        return request;
    }

    @Override
    public int getStatusCode() {
        return fullHttpResponse.status().code();
    }

    @Override
    public HttpResponse setStatusCode(int code) {
        fullHttpResponse.setStatus(HttpResponseStatus.valueOf(code));
        return this;
    }

    @Override
    public void injectJs(Object jsData, boolean force) {
        getContent().injectJs(jsData, force);
    }

    @Override
    public void injectJs(Object jsData) {
        getContent().injectJs(jsData, false);
    }
}