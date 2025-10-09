package cn.iinti.malenia2.api.mitm.http

interface HttpResponse extends HttpMessage {
    // 在response中包装request，并且提供为getter
    HttpRequest getHttpRequest()

    int getStatusCode()

    HttpResponse setStatusCode(int code)

    //////////////////////// 工具方法开始 ///////////////////////////
    /**
     * 在响应中注入一段js代码。
     * <ul>
     *     <li>如果内容为html，则在html中创建一个<javascript>节点,挂载到html最早的节点树上</li>
     *     <li>如果内容为javascript文本，则将jsData放到之前的js内容前面</li>
     * </ul>
     *  如果您希望在原始业务后置挂载注入逻辑，则应该手动调用浏览器函数寻找合适的注入点注册监听事件
     *
     * @param jsData 字符串或者byte[]
     * @param force 强制注入：即是否判断contentType符合预期
     */
    void injectJs(Object jsData, boolean force)

    /**
     * 在响应中注入一段js代码，请注意如果当前内容不是html、js，则不执行js注入
     * @param jsData 字符串或者byte[]
     */
    void injectJs(Object jsData)
}