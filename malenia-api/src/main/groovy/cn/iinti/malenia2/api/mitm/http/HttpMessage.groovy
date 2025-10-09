package cn.iinti.malenia2.api.mitm.http


import cn.iinti.malenia2.api.mitm.data.Content

import javax.annotation.Nullable


/**
 * 和netty保持一致的抽象，让我们可以在脚本中访问netty（即时netty被混淆的情况下）。
 * 请注意如果不考虑netty代码被混淆的话，这里不需要抽象接口的.
 *
 * 相比netty我稍微修改了方法名称，主要目的是让这些内容访问更加适合javaBean规范，这样便于groovy代码里面直接通过访问成员的方式获取数据
 *
 * 在request 闭包调用阶段，闭包的it指向httpRequest 对象，并且httpRequest 解构在当前闭包中
 * 在response闭包调用阶段，闭包的it指向httpResponse对象，并且httpResponse解构在当前闭包中
 */


interface HttpMessage {
    HttpVersion getProtocolVersion()

    HttpMessage setProtocolVersion(HttpVersion version)

    HttpHeaders getHeaders()

    /**
     * 请注意，有些http消息没有body，如GET request
     */
    @Nullable
    byte[] getBytes()

    HttpMessage setBytes(byte[] newContent)

    /**
     * 扩展API，生成请求的完整url
     */
    String getUrl()

    /**
     * 扩展API，用于处理body修改
     */
    Content getContent()
}
