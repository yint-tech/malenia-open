package cn.iinti.malenia2.api.mitm.data

import com.alibaba.fastjson.JSON

import java.nio.charset.StandardCharsets

/**
 * 立即响应返回结构体
 */
class ImmediatelyResponse {
    /**
     * 状态码
     */
    int status = 200

    /**
     * 头部
     */
    Multimap headers = Multimap.create()

    byte[] data

    void setStatus(int status) {
        this.status = status
    }

    void setData(byte[] data) {
        this.data = data
    }

    int getStatus() {
        return status
    }

    Multimap getHeaders() {
        return headers
    }

    byte[] getData() {
        return data
    }

    void json(JSON json) {
        status = 200
        setContentType("application/json;charset=utf8")
        data = json.toString().getBytes(StandardCharsets.UTF_8)
    }


    void addHeader(String key, String value) {
        headers.add(key, value)
    }

    void setHeader(String key, String value) {
        headers.put(key, value)
    }

    void setContentType(String contentType) {
        setHeader("content-type", contentType)
    }
}
