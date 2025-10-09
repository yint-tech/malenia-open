package cn.iinti.malenia2.service.proxy.core.mitm.api.http;

import cn.iinti.malenia2.api.mitm.data.ImmediatelyResponse;
import cn.iinti.malenia2.api.mitm.http.HttpRequest;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.mitm.api.ApiImpl;
import groovy.lang.Closure;
import groovy.lang.GroovyRuntimeException;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Getter;
import lombok.Setter;

public class HttpRequestImpl extends HttpMessageImpl implements HttpRequest {
    @Getter
    @Setter
    private FullHttpRequest fullHttpRequest;
    @Getter
    private final Recorder logger;
    private final boolean isHttps;
    @Getter
    private ImmediatelyResponse immediatelyResponse;

    public HttpRequestImpl(FullHttpRequest fullHttpRequest, Recorder logger, boolean isHttps) {
        super(fullHttpRequest);
        this.isHttps = isHttps;
        this.fullHttpRequest = fullHttpRequest;
        this.logger = logger;
    }

    @Override
    public String getMethod() {
        return fullHttpRequest.method().name();
    }

    @Override
    public String getUri() {
        return fullHttpRequest.uri();
    }

    @Override
    public HttpRequest setUri(String uri) {
        fullHttpRequest.setUri(uri);
        return this;
    }

    @Override
    public boolean isHttps() {
        return isHttps;
    }

    @Override
    public void responseImmediately(Closure<?> closure) {
        ApiImpl.static_log("this request will response immediately");
        immediatelyResponse = new ImmediatelyResponse();
        try {
            Closure<?> newClosure = closure.rehydrate(immediatelyResponse, closure.getOwner(), closure.getThisObject());
            newClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
            newClosure.call();
        } catch (GroovyRuntimeException throwable) {
            immediatelyResponse = null;
            throw throwable;
        }
    }


}
