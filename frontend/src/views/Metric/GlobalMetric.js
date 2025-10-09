import React from 'react';
import MetricPage from "./MetricPage";

const systemMQL = [
    {
        title: "产品宽带(M)",
        mql: ` 
bandwidth = metric(malenia.product.bandwidthNoUser);
带宽 = aggregate(bandwidth,'serverId','productId') * 8 /(1024 * 1024);

show(带宽);
        `
    }, {
        title: "流量(K)",
        mql: `
flow = metric(malenia.product.flow);
流量 = aggregate(flow,'serverId','user','productId') / 1024;
show(流量);
        `
    }, {
        title: "连接",
        bottomLegend: true,
        mql: `
connect = metric(malenia.product.connect);
连接量 = aggregate(connect,'serverId','user','productId');
show(连接量);
        `
    }
]

const GlobalMetrics = () => {
    return (<MetricPage configs={systemMQL}/>);
}

export default GlobalMetrics;