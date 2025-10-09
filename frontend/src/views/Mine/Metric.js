import React, {useContext} from "react";
import {AppContext} from "adapter";
import MetricPage from "../Metric/MetricPage";

const UserMetric = () => {
    const {user} = useContext(AppContext);

    return (<MetricPage configs={[
        {
            title: "宽带(M)",
            mql: ` 
bandwidth = metric(malenia.product.bandwidth[user='${user.userName}']);
带宽 = aggregate(bandwidth,'serverId','productId') * 8 /(1024 * 1024);

show(带宽);
        `
        }, {
            title: "流量(K)",
            mql: `
flow = metric(malenia.product.flow[user='${user.userName}']);
流量 = aggregate(flow,'serverId','productId') / 1024;
show(流量);
        `
        }, {
            title: "连接",
            bottomLegend: true,
            mql: `
connect = metric(malenia.product.connect4User_${user.userName});
连接量 = aggregate(connect,'serverId','sourceKey','productId');
show(连接量);
        `
        }
    ]}/>);
}

export default UserMetric;