package cn.iinti.malenia2.service.proxy.client;

import cn.iinti.malenia2.service.base.safethread.ValueCallback;
import cn.iinti.malenia2.service.base.trace.Recorder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;

import java.net.URL;

/**
 * 早期使用的是： <a href="https://github.com/AsyncHttpClient/async-http-client/">AsyncHttpClient</a>
 * <ul>
 *     <li>bug多：https://github.com/AsyncHttpClient/async-http-client/issues/1913</li>
 *     <li>使用的NettyAPI非常底层导致很容易和其他用Netty的组件冲突(各种要求特定版本netty)</li>
 *     <li>有很多有很多没用的封装：jetty、netty、tomcat很多不知道的框架集成，同时将netty的API做了一次包装抽象</li>
 *     <li>有内存泄漏：在Malenia使用期间，发现偶尔有AsyncHttpClient的内存泄漏，并且已经明确使用方式很简单，不是使用方法不对带来的泄漏</li>
 * </ul>
 * <p>
 * 当malenia的基础环境升级到jdk17以后，我测试发现所有版本的AsyncHttpClient都会触发1913号bug，且此bug官方长达一年时间都没有修复。
 * 基于这些原因，我剥离AsyncHttpClient，手动实现一份，使用手写的组件平替AsyncHttpClient
 */
public class AsyncHttpInvoker {
    public static void get(String url, Recorder recorder, ProxyInfo proxyInfo, ValueCallback<String> callback) {
        HttpClient.execute(url, recorder, proxyInfo, callback, () -> {
            URL parseURL = HttpClient.parseURL(url);
            return HttpClient.createRequest(HttpMethod.GET, parseURL, proxyInfo);
        });
    }

    public static void get(String url, Recorder recorder, ValueCallback<String> callback) {
        get(url, recorder, null, callback);
    }

    public static void post(String url, String body, String contentType, Recorder recorder,
                            ValueCallback<String> callback) {
        recorder.recordEvent(() -> "begin async invoker post: " + url + " content:" + body);
        HttpClient.execute(url, recorder, null, callback, () -> {
            URL parseURL = HttpClient.parseURL(url);
            DefaultFullHttpRequest httpRequest = HttpClient.createRequest(HttpMethod.POST, parseURL, null);
            httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType);
            return httpRequest;
        });
    }

}
