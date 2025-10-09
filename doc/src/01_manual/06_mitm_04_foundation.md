# 底层概念
脚本的运行环境是受限的，这是因为框架开放的扩展能力需要被控制，这些限制包括安全问题、性能问题、资源隔离问题等。

## 线程池

mitm脚本不会运行在malenia核心线程中，所有mitm脚本运行在一个单独线程池中。如果用户在malenia线程池中执行耗时动作，并且导致线程池被打满，这可能mitm的代码无法提交， malenia框架对这种情况的控制逻辑有两个。

1. 取消mitm：mitm功能失效，但是流量转发依然正常执行
2. 阻断整个mitm的请求隧道，直接报告隧道失败

目前默认的策略为：``取消mitm`` ,你可以通过config脚本配置控制这个开关``abortWhenSubmitFailed``，如下：

```groovy
sniff("www.baidu.com", {
    config {
        // 如果如果失败，则阻断当前请求
        abortWhenSubmitFailed = true
    }
    request {
        // do intercept for http data
    }

    response {
        // do intercept for http data
    }
})
```

### 二级线程池

正常情况下，所有的用户脚本公用同一个线程池资源，公共线程池资源相对较多。但是可能有某个用户错误的编写代码，则可能导致这个公共线程池资源被打满。 为了实现用户隔离和资源复用，
当公共线程池被打满之后，我们会为用户空间创建一个小型线程池，并使用这个小型线程池执行mitm脚本。这个小型线程池则是每个用户独有， 如此保证在某个特定用户的错误代码影响其他的用户

### 线程池配置

你可以在超级管理员 -> 用户设置 设置mitm线程池的大小

## groovy代码安全

扩展脚本是给用户使用的编程扩展，但是当一个应用可以编程的时候，他可以做很多事情，这些事情则可以带来巨大的安全问题。比如这里groovy脚本可以直接读取文件，执行shell等，这可以让普通用户获取服务器的权限。
所以有必要限制groovy脚本的行为，对编程API做一些约束。为此我们限制了groovy代码中的部分API。这些API有

1. java.io.File:
2. java.nio.file.Files:
3. java.io.FileInputStream:
4. java.io.FileOutputStream:
5. java.lang.Runtime:
6. java.net.URLConnection:
7. sun.net.www.protocol.http.HttpURLConnection
8. javax.net.ssl.HttpsURLConnection
9. sun.net.www.protocol.https.HttpsURLConnectionImpl
10. java.net.Socket
11. javax.net.ssl.SSLSocket
12. com.sun.rowset.JdbcRowSetImpl

上述jdk提供的API，不允许扩展脚本中使用，脚本中对这些api的访问将会导致脚本直接编译报错。

