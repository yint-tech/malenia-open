# NodeJs

## axios-https-proxy-fix
```javascript
const axios = require('axios-https-proxy-fix');
const proxyUser = 'yourProxyAccount' //代理用户名
const proxySecret = 'yourProxyPassword' //代理密码
//要访问的网站页面,最好访问那些能够返回公网ip的url以验证是否使用成功,如http://myip.ipip.net/
let targetUrl = "http://myip.ipip.net/";

let proxy = {
      host: 'malenia.iinti.cn',
      port: 24000,
      auth: {
        username: proxyUser,
        password: proxySecret
      }
    };

axios.get(targetUrl,{proxy:proxy})
      .then(function (response) {
        console.log('返回结果为：')
        console.log(response.data);
      })
      .catch(function (error) {
        console.log(error);
      })
```