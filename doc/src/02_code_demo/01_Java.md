# java
## okhttp3
```java
import okhttp3.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

public class Test {
    public static final String proxyUser = "yourProxyAccount";//代理账号
    public static final String proxyPass = "yourProxyPassword";//代理密码
    public static final String proxyHost = "malenia.iinti.cn";
    public static final int proxyPort = 24000;

    public static void main(String[] args) throws IOException {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

        Authenticator proxyAuthenticator = new Authenticator() {
            public Request authenticate(Route route, Response response) {
                String credential = Credentials.basic(proxyUser, proxyPass);
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            }
        };

        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .proxy(proxy)
                .proxyAuthenticator(proxyAuthenticator)
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .build();
        // 要访问的目标页面,最好选择返回公网ip的url来验证是否使用成功
        String targetUrl = "http://myip.ipip.net";

        Request request = new Request.Builder()
                .url(targetUrl)
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());
    }
}

```

## httpclient
``` java

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Test {
    public static final String proxyUser = "yourProxyAccount";//代理账号
    public static final String proxyPass = "yourProxyPassword";//代理密码
    public static final String proxyHost = "malenia.iinti.cn";
    public static final int proxyPort = 24000;

    public static void main(String[] args) throws IOException {
        // 创建Httpclient对象
        HttpHost proxy = new HttpHost(proxyHost, proxyPort, HttpHost.DEFAULT_SCHEME_NAME);
        CredentialsProvider provider = new BasicCredentialsProvider();
        //包含账号密码的代理
        provider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(proxyUser, proxyPass));
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(provider).build();
        RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
        CloseableHttpResponse response = null;
        String resultString = "";
        String targetUrl = "http://myip.ipip.net";
        try {
            // 创建Http Get请求
            HttpGet httpGet = new HttpGet(targetUrl);
            httpGet.setConfig(config);
            // 执行http请求
            response = httpClient.execute(httpGet);
            resultString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(response);
        }
        System.out.println("返回url:" + targetUrl + ",response:" + resultString);
    }
}

```

## HttpURLConnection
```java

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Test {
    public static final String proxyUser = "yourProxyAccount";//代理账号
    public static final String proxyPass = "yourProxyPassword";//代理密码
    public static final String proxyHost = "malenia.iinti.cn";
    public static final int proxyPort = 24000;

    public static void main(String[] args) throws IOException {
        //创建代理服务器
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        //设置代理的用户名密码
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
            }
        });

        String targetUrl = "http://myip.ipip.net";

        // 设定连接的相关参数
        URL url = new URL(targetUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        String s = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        System.out.println("请求结果：" + s);
    }
}
```