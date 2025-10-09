import React from 'react';
import MetricPage from "../../Metric/MetricPage";


const SystemMetrics = ({productId}) => {
    const systemMQL = [
        {
            title: "产品带宽(M)",
            mql: ` 
bandwidth = metric(malenia.product.bandwidthNoUser[productId='${productId}']);
带宽 = aggregate(bandwidth,'serverId') * 8 /(1024 * 1024);

show(带宽);
        `
        }, {
            title: "流量(K)",
            mql: `
flow = metric(malenia.product.flow[productId='${productId}']);
流量 = aggregate(flow,'serverId','user') / 1024;
show(流量);
        `
        }, {
            title: "连接",
            bottomLegend: true,
            mql: `
connect = metric(malenia.product.connect[productId='${productId}']);
连接量 = aggregate(connect,'serverId','user');
show(连接量);
        `
        }
    ]
    return (<div>
        <MetricPage configs={systemMQL}/>
    </div>);
}

export default SystemMetrics;