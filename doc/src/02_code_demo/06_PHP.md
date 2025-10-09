# PHP
## curl
```php
$targetUrl = "http://myip.ipip.net";

$proxyUser = "yourProxyAccount"; //代理用户名
$proxySecret = "yourProxyPassword"; // 代理密码

$proxyIp = "malenia.iinti.cn";
$proxyPort = 24000;
$proxyServer = "$proxyIp:$proxyPort";
var_dump('要使用的代理ip为:'.$proxyServer);

//2.使用：使用上面提取到的ip
curl_setopt($ch, CURLOPT_URL, $targetUrl);
curl_setopt($ch, CURLOPT_PROXY, $proxyServer);
curl_setopt($ch, CURLOPT_PROXYTYPE, CURLPROXY_HTTP); //http
//curl_setopt($ch, CURLOPT_PROXYTYPE, CURLPROXY_SOCKS5); //socks5
curl_setopt($ch, CURLOPT_PROXYAUTH, CURLAUTH_BASIC);
curl_setopt($ch, CURLOPT_PROXYUSERPWD, "$proxyUser:$proxySecret");
curl_setopt($ch, CURLOPT_USERAGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 3);
curl_setopt($ch, CURLOPT_TIMEOUT, 5);
curl_setopt($ch, CURLOPT_HEADER, 0);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
$result = curl_exec($ch);
curl_close($ch);
var_dump('访问结果为:'.$result);

```

## stream
```
//要访问的网站页面,最好访问那些能够返回公网ip的url以验证是否使用成功,如http://myip.ipip.net/
$targetUrl = "http://myip.ipip.net/";

$proxyUser = "yourProxyAccount"; //代理用户名
$proxySecret = "yourProxyPassword"; // 代理密码

$proxyIp = "malenia.iinti.cn";
$proxyPort = 24000;
$proxyServer = "$proxyIp:$proxyPort";
var_dump('要使用的代理ip为:'.$proxyServer);

$proxyAuth = base64_encode("{$proxyUser}:{$proxySecret}");
$headers = implode("\r\n", [
    "Proxy-Authorization: Basic {$proxyAuth}",
]);
$options = [
    "http" => [
        "proxy"  => $proxyServer,
        "header" => $headers,
        "method" => "GET",
    ],
];
$context = stream_context_create($options);
$result = file_get_contents($targetUrl, false, $context);
var_dump('访问结果为:'.$result);

```