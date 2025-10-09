package cn.iinti.malenia2.api

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.SecureASTCustomizer
import sun.net.www.protocol.http.HttpURLConnection
import sun.net.www.protocol.https.HttpsURLConnectionImpl

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import java.nio.file.Files

class ScriptFactory {
    static <T extends Script> T compileScript(String groovyScriptSource, Class<T> clazz) {
        // 控制脚本的行为，有一些api不让脚本访问，特别是命令行，文件等
        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer()
        List<Class> disallow = new ArrayList<Class>()
        disallow.add(File.class)// 如果开放文件读写，则可能让他们直接读写操作系统的文件，很多同学都是直接使用root运行代码的
        disallow.add(Files.class)
        disallow.add(FileInputStream.class)
        disallow.add(FileOutputStream.class)
        disallow.add(Runtime.class)// 同理，exec函数在这里实现
        disallow.add(URLConnection.class)// 不允许他直接访问网络，这可能导致框架不稳定，毕竟网络操作非常耗时
        try {
            // 这些可能在扩展包里面，没有不报错
            disallow.add(HttpURLConnection.class)
            disallow.add(HttpsURLConnection.class)
            disallow.add(HttpsURLConnectionImpl.class)
        } catch (Throwable ignore) {
            //ignore
        }

        disallow.add(Socket.class)// 同理
        try {
            disallow.add(SSLSocket.class)
        } catch (Throwable ignore) {
            //ignore
        }
        try {
            // fastjson的漏洞，通过他加载rmi，然后被shell注入了
            Class JdbcRowSetImplClass = ClassLoader.forName("com.sun.rowset.JdbcRowSetImpl")
            disallow.add(JdbcRowSetImplClass)
        } catch (Throwable ignore) {
            //ignore
        }
        secureASTCustomizer.with {
            disallowedReceiversClasses = disallow
        }
        def configuration = new CompilerConfiguration()
        configuration.addCompilationCustomizers(secureASTCustomizer)
        configuration.scriptBaseClass = clazz.name
        def shell = new GroovyShell(configuration)
        def script
        if (groovyScriptSource instanceof File) {
            script = shell.parse(groovyScriptSource as File)
        } else if (groovyScriptSource instanceof URI) {
            script = shell.parse(groovyScriptSource as URI)
        } else if (groovyScriptSource instanceof GroovyCodeSource) {
            script = shell.parse(groovyScriptSource as GroovyCodeSource)
        } else {
            script = shell.parse(groovyScriptSource.toString())
        }
        script as T
    }
}
