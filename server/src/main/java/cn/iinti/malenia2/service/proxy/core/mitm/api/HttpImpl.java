package cn.iinti.malenia2.service.proxy.core.mitm.api;

import cn.iinti.malenia2.api.tools.Http;
import cn.iinti.malenia2.utils.net.SimpleHttpInvoker;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpImpl implements Http {

    @Override
    public String get(String url) {
        return SimpleHttpInvoker.get(url);
    }

    @Override
    public byte[] getEntity(String url) {
        return SimpleHttpInvoker.getEntity(url);
    }

    @Override
    public String post(String url, JSONObject body) {
        return SimpleHttpInvoker.post(url, body);
    }

    @Override
    public String post(String url, Map<String, String> body) {
        return SimpleHttpInvoker.post(url, body);
    }

    @Override
    public byte[] execute(String method, String url, byte[] body) {
        return SimpleHttpInvoker.execute(method, url, body);
    }

    @Override
    public String asString(byte[] data) {
        return SimpleHttpInvoker.asString(data);
    }

    @Override
    public IOException getIoException() {
        return SimpleHttpInvoker.getIoException();
    }

    @Override
    public int getResponseStatus() {
        return SimpleHttpInvoker.getResponseStatus();
    }

    @Override
    public void addHeader(String key, String value) {
        SimpleHttpInvoker.addHeader(key, value);
    }

    @Override
    public LinkedHashMap<String, String> getResponseHeader() {
        return SimpleHttpInvoker.getResponseHeader();
    }

    @Override
    public void setTimout(int connectTimout, int readTimeout) {
        SimpleHttpInvoker.setTimout(connectTimout, readTimeout);
    }
}