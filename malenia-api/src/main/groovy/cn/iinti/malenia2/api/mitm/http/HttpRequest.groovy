package cn.iinti.malenia2.api.mitm.http

import cn.iinti.malenia2.api.mitm.data.ImmediatelyResponse


interface HttpRequest extends HttpMessage {
    String getMethod()

    String getUri()

    HttpRequest setUri(String uri)

    boolean isHttps()

    /**
     * 直接返回，不经过上级代理服务器和最终服务器请求
     * @param closure
     */
    void responseImmediately(@DelegatesTo(ImmediatelyResponse) Closure<?> closure)
}
