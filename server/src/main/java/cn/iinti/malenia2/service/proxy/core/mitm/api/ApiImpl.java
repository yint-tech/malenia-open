package cn.iinti.malenia2.service.proxy.core.mitm.api;

import cn.iinti.malenia2.api.tools.Api;
import cn.iinti.malenia2.api.tools.AsyncHttp;
import cn.iinti.malenia2.api.tools.Http;
import cn.iinti.malenia2.service.base.storage.StorageManager;
import cn.iinti.malenia2.service.base.trace.Recorder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class ApiImpl implements Api {
    private final String user;
    private static final ThreadLocal<Recorder> recorderThreadLocal = new ThreadLocal<>();

    public static void setupLogger(Recorder recorder) {
        recorderThreadLocal.set(recorder);
    }

    public static void cleanLogger() {
        recorderThreadLocal.remove();
    }

    public ApiImpl(String user) {
        this.user = user;
    }

    @Override
    public byte[] getResource(String key) {
        static_log("get asset for :" + key);
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        File file = StorageManager.get("mitmAsset/" + user + "/" + key);
        if (file == null || !file.exists()) {
            log("file not  exist: " + key);
            return null;
        }
        try {
            return FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            log("read filed failed", e);
            return null;
        }
    }

    private static final HttpImpl http = new HttpImpl();

    @Override
    public Http getHttp() {
        return http;
    }

    private final AsyncHttpImpl asyncHttp = new AsyncHttpImpl();

    @Override
    public AsyncHttp getAsyncHttp() {
        return asyncHttp;
    }

    @Override
    public void log(String msg) {
        static_log(msg);
    }

    @Override
    public void log(String msg, Throwable throwable) {
        static_log(msg, throwable);
    }

    public static Recorder fetchRecorder() {
        Recorder recorder = recorderThreadLocal.get();
        if (recorder == null) {
            recorder = Recorder.nop;
        }
        return recorder;
    }


    public static void static_log(String msg) {
        fetchRecorder().recordEvent(msg);
    }

    public static void static_log(String msg, Throwable throwable) {
        fetchRecorder().recordEvent(msg, throwable);
    }

}
