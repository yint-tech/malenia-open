package cn.iinti.malenia2.api.tools;

import groovy.lang.Closure;

/**
 * 异步http，建议大家使用异步
 */
public interface AsyncHttp {
    void get(String url, Closure<CallbackContext> callback);

    void post(String url, Object body, String contentType, Closure<CallbackContext> callback);

    void post(String url, Object body, Closure<CallbackContext> callback);

    /**
     * 返回的callback
     */
    interface CallbackContext {

        boolean isSuccess();

        String getResult();

        Throwable getCause();
    }
}