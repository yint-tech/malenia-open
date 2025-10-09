package cn.iinti.malenia2.service.proxy.core.mitm.api;

import cn.iinti.malenia2.api.tools.AsyncHttp;
import cn.iinti.malenia2.service.base.safethread.ValueCallback;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.client.AsyncHttpInvoker;
import com.alibaba.fastjson.JSON;
import groovy.lang.Closure;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AsyncHttpImpl implements AsyncHttp {

    @Override
    public void get(String url, Closure<AsyncHttp.CallbackContext> callback) {
        AsyncHttpInvoker.get(url, ApiImpl.fetchRecorder(), createCallback(callback));
    }

    @Override
    public void post(String url, Object body, String contentType, Closure<AsyncHttp.CallbackContext> callback) {
        AsyncHttpInvoker.post(url, body.toString(), contentType, ApiImpl.fetchRecorder(), createCallback(callback));
    }

    @Override
    public void post(String url, Object body, Closure<AsyncHttp.CallbackContext> callback) {
        String contentType;
        if (body instanceof Map) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) body).entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                sb.append(URLEncoder.encode(key.toString(), StandardCharsets.UTF_8));
                sb.append("=");
                if (value != null) {
                    sb.append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
                }
                sb.append("&");
            }
            contentType = "application/x-www-form-urlencoded;charset=utf8";
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            body = sb.toString();
        } else {
            body = JSON.toJSON(body);
            contentType = "application/json;charset=utf8";
        }

        AsyncHttpInvoker.post(url, body.toString(), contentType, ApiImpl.fetchRecorder(), createCallback(callback));
    }

    private static ValueCallback<String> createCallback(Closure<AsyncHttp.CallbackContext> callback) {
        Recorder recorder = ApiImpl.fetchRecorder();
        return value -> {
            Recorder tmpLogger = ApiImpl.fetchRecorder();
            ApiImpl.setupLogger(recorder);

            try {
                CallbackContext callbackContext = new CallbackContext(value.v, value.e);
                Closure<AsyncHttp.CallbackContext> closure = callback.rehydrate(callbackContext, callback.getOwner(), callback.getOwner());
                closure.setResolveStrategy(Closure.DELEGATE_FIRST);
                closure.call(callbackContext);
            } finally {
                if (tmpLogger == null) {
                    ApiImpl.cleanLogger();
                }
            }
        };
    }


    private static class CallbackContext implements AsyncHttp.CallbackContext {

        private final String result;
        private final Throwable throwable;

        public CallbackContext(String result, Throwable throwable) {
            this.result = result;
            this.throwable = throwable;
        }

        @Override
        public boolean isSuccess() {
            return throwable == null;
        }

        @Override
        public String getResult() {
            return result;
        }

        @Override
        public Throwable getCause() {
            return throwable;
        }
    }

}