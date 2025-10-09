package cn.iinti.malenia2.api.tools;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public interface Http {
    // http系列API,为了接口优雅，扩展数据来自
    String get(String url);

    byte[] getEntity(String url);

    String post(String url, JSONObject body);

    String post(String url, Map<String, String> body);

    byte[] execute(String method, String url, byte[] body);

    String asString(byte[] data);

    IOException getIoException();

    int getResponseStatus();

    void addHeader(String key, String value);

    LinkedHashMap<String, String> getResponseHeader();

    void setTimout(int connectTimout, int readTimeout);
}