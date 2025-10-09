package cn.iinti.malenia2.service.base.alert;

import cn.iinti.malenia2.service.base.alert.events.DiskPoorEvent;
import cn.iinti.malenia2.service.base.alert.events.IpSourceHealthScoreLowEvent;
import cn.iinti.malenia2.service.base.alert.events.MetricMonitorConfig;
import cn.iinti.malenia2.service.base.alert.events.SensitiveOperationEvent;
import cn.iinti.malenia2.utils.net.SimpleHttpInvoker;
import com.alibaba.fastjson.JSONObject;
import groovy.json.JsonBuilder;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public abstract class EventScript extends Script {
    public List<Closure<?>> sensitiveEventList = new ArrayList<>();

    public List<Closure<?>> diskPoorEventList = new ArrayList<>();

    public List<MetricMonitorConfig> metricMonitorConfigList = new ArrayList<>();

    public List<Closure<?>> ipSourceHealthScoreLowEventList = new ArrayList<>();

    /**
     * 敏感操作事件，被标记了alert的接口，当存在用户对他进行访问时，此扩展脚本将会被调用
     *
     * @see cn.iinti.malenia2.system.LoginRequired
     */
    public void onSensitiveOperation(@DelegatesTo(SensitiveOperationEvent.class) Closure<?> closure) {
        sensitiveEventList.add(closure);
    }

    /**
     * 当系统磁盘少于75%的时候，此扩展接口将会被调用
     */
    public void onDiskPoor(@DelegatesTo(DiskPoorEvent.class) Closure<?> closure) {
        diskPoorEventList.add(closure);
    }

    public void onIpSourceHealthScoreLow(@DelegatesTo(IpSourceHealthScoreLowEvent.class) Closure<?> closure) {
        ipSourceHealthScoreLowEventList.add(closure);
    }

    public void mqlSubscribe(@DelegatesTo(MetricMonitorConfig.class) Closure<?> closure) {
        MetricMonitorConfig metricMonitorConfig = new MetricMonitorConfig();
        closure.rehydrate(metricMonitorConfig, closure.getOwner(), closure.getThisObject())
                .call();
        metricMonitorConfig.valid();
        metricMonitorConfigList.add(metricMonitorConfig);
    }

    public String httpPost(String url, JsonBuilder jsonBuilder) {
        return SimpleHttpInvoker.post(url, JSONObject.parseObject(jsonBuilder.toString()));
    }


    public static EventScript compileScript(String groovyScriptSource) {
        // 控制脚本的行为，有一些api不让脚本访问，特别是命令行，文件等
        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
        List<Class> disallow = new ArrayList<>();
        disallow.add(File.class);// 如果开放文件读写，则可能让他们直接读写操作系统的文件，很多同学都是直接使用root运行代码的
        disallow.add(Files.class);
        disallow.add(FileInputStream.class);
        disallow.add(FileOutputStream.class);
        disallow.add(Runtime.class);// 同理，exec函数在这里实现
        disallow.add(URLConnection.class);// 不允许他直接访问网络，这可能导致框架不稳定，毕竟网络操作非常耗时
        disallow.add(HttpsURLConnection.class);
        disallow.add(Socket.class);// 同理
        try {
            disallow.add(SSLSocket.class);
        } catch (Throwable ignore) {
            //ignore
        }
        try {
            // fastjson的漏洞，通过他加载rmi，然后被shell注入了
            Class<?> JdbcRowSetImplClass = ClassLoader.getSystemClassLoader().loadClass("com.sun.rowset.JdbcRowSetImpl");
            disallow.add(JdbcRowSetImplClass);
        } catch (Throwable ignore) {
            //ignore
        }
        secureASTCustomizer.setDisallowedReceiversClasses(disallow);

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addCompilationCustomizers(secureASTCustomizer);
        configuration.setScriptBaseClass(EventScript.class.getName());
        GroovyShell shell = new GroovyShell(configuration);
        EventScript t = (EventScript) shell.parse(groovyScriptSource);
        t.run();
        return t;
    }
}
