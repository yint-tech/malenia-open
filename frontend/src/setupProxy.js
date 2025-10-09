const {createProxyMiddleware} = require('http-proxy-middleware');

//const target = "http://localhost:8060/";
const target = "http://malenia.iinti.cn/";

module.exports = function (app) {
    app.use(
        '/malenia-api',
        createProxyMiddleware({
            target: target,
            changeOrigin: true,
        })
    );
    app.use(
        '/malenia-doc',
        createProxyMiddleware({
            target: target,
            changeOrigin: true,
        })
    );
};
