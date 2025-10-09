# Python
## Requests

```python
import requests

# 要访问的url地址：如 http://myip.ipip.net/
targetUrl = "http://myip.ipip.net/"

proxyUser = "yourProxyAccount"
proxyPass = "yourProxyPassword"

proxyHost = "malenia.iinti.cn"
proxyPort = 24000

proxyMeta = f"http://{proxyUser}:{proxyPass}@{proxyHost}:{proxyPort}"

proxies = {
    "http": proxyMeta,
    "https": proxyMeta,
}
print(proxies)
res = requests.get(url=targetUrl, proxies=proxies)

print("访问结果为:", res.text)
# print (res.text.encode('GBK','ignore'))
```

## aiohttp

```python

import aiohttp
import asyncio
from aiohttp_socks import ProxyConnector

proxyUser = "yourProxyAccount"
proxyPass = "yourProxyPassword"

proxyHost = "malenia.iinti.cn"
proxyPort = 24000

proxy_meta = f"http://{proxyUser}:{proxyPass}@{proxyHost}:{proxyPort}"

# 要访问的url地址：如 http://myip.ipip.net/
targetUrl = "http://myip.ipip.net"
max_wait_time = 10
header = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36"
}


async def runProxy(proxy_meta):
    proxyConnector = ProxyConnector.from_url(proxy_meta)
    async with aiohttp.ClientSession(connector=proxyConnector) as session:
        try:
            async with session.get(targetUrl, timeout=max_wait_time, headers=header) as response:
                # body = await response.read()
                body = await response.text()
                print(f"http状态码是:{response.status}")
                print(f"http内容是:{body}")
        except Exception as e:
            print("出现exception")
            print("repr(e):	", repr(e))


asyncio.get_event_loop().run_until_complete(runProxy(proxy_meta))
```