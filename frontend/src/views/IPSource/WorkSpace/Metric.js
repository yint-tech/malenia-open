import React from 'react';
import MetricPage from "../../Metric/MetricPage";


const SystemMetrics = ({ipSourceKey}) => {
    const systemMQL = [
        {
            title: "ip资源入库",
            bottomLegend: true,
            mql: ` 
下载量 = metric(malenia.ipSource.downloadCount[sourceKey='${ipSourceKey}']);
入库前测试 = metric(malenia.ipSource.test[sourceKey='${ipSourceKey}']);
入库重复IP = metric(malenia.ipSource.duplicateOffer[sourceKey='${ipSourceKey}']);

show(下载量,入库前测试,入库重复IP);
        `
        }, {
            title: "池化状态",
            bottomLegend: true,
            mql: `
备用IP量 = metric(malenia.ipSource.ipPool.cacheSize[sourceKey='${ipSourceKey}']);
IP池大小 = metric(malenia.ipSource.ipPool.poolSize[sourceKey='${ipSourceKey}']);

show(备用IP量,IP池大小);
        `
        }, {
            title: "健康指数",
            bottomLegend: true,
            mql: `
健康指数 = metric(malenia.ipSource.ipPool.health[sourceKey='${ipSourceKey}']);

show(健康指数);
        `
        },{
            title: "IP分发",
            bottomLegend: true,
            mql: `
ip分发请求 = metric(malenia.ipSource.ipPool.allocate[sourceKey='${ipSourceKey}']);

show(ip分发请求);
        `
        }, {
            title: "连接",
            bottomLegend: true,
            mql: `
tcp连接 = metric(malenia.ipSource.ipPool.connectToIp[sourceKey='${ipSourceKey}']);
连接池命中 = metric(malenia.ipSource.ipPool.connCacheHint[sourceKey='${ipSourceKey}']);

show(tcp连接,连接池命中);
        `
        }, {
            title: "失败原因",
            bottomLegend: true,
            mql: `
失败原因 = metric(malenia.ipSource.ipPool.connFailed[sourceKey='${ipSourceKey}']);

show(失败原因);
        `
        }
    ]
    return (<div>
        <MetricPage configs={systemMQL}/>
    </div>);
}

export default SystemMetrics;