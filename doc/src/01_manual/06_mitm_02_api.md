# mitmAPI

请注意，所有api的定义都是在集成环境中实现的。你可以直接阅读mitm dsl集成环境的代码知道更加详细的API和代码设计

## api

这是整个脚本作用域有效的一个对象，他提供malenia框架的静态工具，你可以参考集成环境中``Api.groovy``文件查看这些函数的定义。如：

```groovy
// 获取资产文件数据
byte[] data = api.getResource('/baidu_inject_logo.png')
// 调用http接口
String response = api.http.get("https://www.baidu.com")

// 异步http（建议使用异步）
api.asyncHttp.get("https://www.baidu.com", {
    if (sucess) {
        //访问成功，这时可以获取result对象，他是一个响应的string
        api.log("upload success" + result)
    } else {
        // 访问失败，这是可以获得cause对象，他是一个throwable
        api.log("upload failed", cause)
    }
})
```

## intercept

intercept是Mitm的函数入口，每一个脚本中都可以定义多个interceptor。你可以参考集成环境中``Interceptor.groovy``文件查看这些函数的定义。 如

```groovy
// 定义第一个拦截器
intercept {
    config {
        sniff "www.baidu.com"
        sniff "www.weibo.com"
        sniff "www.tencent.com"
    }

    request {

    }

    response {

    }
}
// 定义第二个拦截器（一个脚本中可以定义多个拦截器，）
intercept {
    config {
        //xxxx
    }
    request {
        //xxx
    }
}
```

intercept闭包下，定义了三个API，分别为``config``、``request``、``response``，他们都是接收闭包作为规则参数。

### intercept.config

config用于配置这一个拦截器的拦截参数，他的定义在文件: ``Config.groovy``中，拥有如下API

1. directSend: 标记命中规则的流量，不会使用上级代理。而是以malenia作为最终的代理出口。但是他依然会经过mitm拦截器
2. abortWhenSubmitFailed： 在拦截器线程被堵塞打满之后的拦截器拒绝策略，一般情况下只要你不在脚本中做耗时操作（如有必要http一定要使用异步）。那么几乎不需要关心这个参数
3. sniff： 定义当前拦截器对那些流量生效，**sniff至少配置一项，缺失配置的脚本将会编译失败**。请注意这个判断在ssl解密之前，所以他是基于domain:port判定的。你可以使用通配符
4. product: 定义当前拦截器对应的代理产品，不指定则代表对当前账户下的所有代理请求生效

#### sniff表达式

为了方便描述目标网站的过滤规则，这里的sniff表达式可以不是全称的域名。你可以使用如下的规则列表:

1. www.baidu.com:80 指定百度的http站
2. www.baidu.com 指定百度的所有端口
3. *.baidu.com 百度公司的所有网站
4. \*.weibo.com|\*.sina.com|\*.sina.cn :新浪旗下所有网站

### intercept.request
代表http请求的拦截处理函数，你可以在这里修改http请求内容，同时你可以将request的数据上传到某个URL。
request的委托对象的定义在文件:``HttpRequest.groovy``,这里有大量的关于http对象的数据和操作API。由于方法太多，这里不做一一解释，请大家参考对应文件

### intercept.response
代表http响应的拦截处理函数，你可以在这里修改http返回内容，同时你可以将response的数据上传到某个URL。
response的委托对象的定义在文件:``HttpResponse.groovy``,这里有大量的关于http对象的数据和操作API。由于方法太多，这里不做一一解释，请大家参考对应文件

