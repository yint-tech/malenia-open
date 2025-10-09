# c#
## 使用 RestSharp（版本106.5.4）

```csharp

// See https://aka.ms/new-console-template for more information

using System.Net;
using RestSharp;
// ProxyAuth 账号密码
var defaultCredential = new NetworkCredential("yourProxyAccount", "yourProxyPassword");
var proxyPorts = "24000-24400"; // 端口范围
var proxyHost = "malenia.iinti.cn";// 隧道代理域名
var startPort = int.Parse(proxyPorts.Split("-")[0]);
var endPort = int.Parse(proxyPorts.Split("-")[1]);
// 随机端口（每个端口对应五分钟内固定出口IP）
int randomPort = new Random().Next(startPort, endPort);
var currentWebProxy = new WebProxy(proxyHost, randomPort) { Credentials = defaultCredential };

// RestSharp https://www.nuget.org/packages/RestSharp
var client = new RestClient("https://www.baidu.com/") { Proxy = currentWebProxy };
var request = new RestRequest() { Timeout = 30 * 1000 };
var response = client.Execute(request, Method.GET);
Console.WriteLine(response.Content);


```
