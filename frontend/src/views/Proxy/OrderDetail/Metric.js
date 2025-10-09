import React, {useContext} from 'react';
import MetricPage from "../../Metric/MetricPage";
import {AppContext} from "adapter";

const SystemMetrics = ({productId}) => {
    const {user} = useContext(AppContext);
    const systemMQL = [
        {
            title: "宽带(M)",
            mql: ` 
bandwidth = metric(malenia.product.bandwidth[productId='${productId}'][user='${user.userName}']);
带宽 = aggregate(bandwidth,'serverId') * 8 /(1024 * 1024);

show(带宽);
        `
        }, {
            title: "流量(K)",
            mql: `
flow = metric(malenia.product.flow[productId='${productId}'][user='${user.userName}']);
流量 = aggregate(flow,'serverId') / 1024;
show(流量);
        `
        }, {
            title: "连接",
            bottomLegend: true,
            mql: `
connect = metric(malenia.product.connect4User_${user.userName}[productId='${productId}']);
连接量 = aggregate(connect,'serverId');
show(连接量);
        `
        }
    ]
    return (<div>
        <MetricPage configs={systemMQL}/>
    </div>);
}

export default SystemMetrics;