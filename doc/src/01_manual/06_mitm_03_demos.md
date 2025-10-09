# demo样例

## 返回固定资产文件
```groovy
// 使用资源文件中的文件，替换返回内容，而不经过转发。
// 当我们处理视频网站或者视频app的元数据拦截上报的时候，视频内容本身可能没有意义，所以我们可以通过返回一个固定文件的方法，
// 减少代理资源的使用负担，也让我们返回的数据更快
intercept {
    config {
        sniff "www.baidu.com"
    }
    request {
        if (uri.endsWith("plus_logo_web_2.png")) {
            responseImmediately {
                data = api.getResource('/baidu_inject_logo.png')
                contentType = "image/jpeg"
            }
        }
    }
}
```


## 注入js到网页
```groovy
// 本案例主要讲解如何使用js注入指令，js注入指令是一个直接将一段js代码注入到网页中的指令
// 请注意如果你使用资产文件的话，你需要提前将资产文件上传到服务器中
// 本案例是malenia和sekiro的配合使用实战案例，他通过malenia解决三个问题
//   1. 解决头条不允许跨域连接sekiro的问题
//   2. 自动将sekiro和rpc客户端js代码注入到头条的网页中
//   3. 自动让浏览器集群集成代理ip池，让每个浏览器走不同的ip出口
intercept {
    config {
        sniff "www.toutiao.com"
        // 我们暂时不支持http2.0，所以关闭头条的http2，后面再支持他
        disableH2 = true
    }
    response {
        if (url == "https://www.toutiao.com/") {

            // 注入js文件到头条首页
            injectJs api.getResource("/sekiro_web_client.js")

            // 允许websockt跨域连接到任何网站
            def origin = headers.get("content-security-policy")
            headers.set("content-security-policy","connect-src  *; "+ origin)

            def originReport = headers.get("content-security-policy-report-only")
            headers.set("content-security-policy-report-only","connect-src *; "+ originReport)
        }
    }
}

intercept {
    config {
        // sekiro的请求，不走二级代理
        sniff "sekiro.virjar.com"
        directSend = true
    }
}
```

## 收集网页cookie
```groovy
// 以下案例，我们监控服务器的返回内容，并且拦截服务器返回的cookie数据，然后使用http的api将数据发送到指定服务器
intercept {
    config {
        sniff "xxxx.xxx.com"
    }
    response {
        def cookie = headers.get("Set-Cookie")
        if (cookie) {
            api.asyncHttp.post(
                    "http://www.mycompany.com/xxxService/uploadCookie",
                    [
                            data: headers.get("Set-Cookie")
                    ],
                    {
                        if (sucess) {
                            api.log("upload success" + result)
                        } else {
                            api.log("upload failed", cause)
                        }
                    },)
        }
    }
}
```
## 上报数据
```groovy
import com.alibaba.fastjson2.JSONPath

// 本案例主要讲解如何使用js注入指令，js注入指令是一个直接将一段js代码注入到网页中的指令
// 请注意如果你使用资产文件的话，你需要提前将资产文件上传到服务器中
intercept {
    config {
        sniff "web.com"
    }
    response {
        if (uri.endWith("xxx/xxx.json")) {
            api.asyncHttp.post(
                    "http://www.mycompany.com/xxxService/uploadCookie",
                    JSONPath.eval(content.json(), '$.data.Goods'), {
                if (sucess) {
                    api.log("upload success" + result)
                } else {
                    api.log("upload failed", cause)
                }
            })
        }
    }
}
```

## 直接转发
```groovy
// 下面脚本内容的含义是，指向：sekiro.virjar.com的链接，不使用上级代理转发
// 而是直接发送给目标服务器
intercept {
    config {
        sniff "sekiro.virjar.com"
        direct
    }
}
```