import React, {useContext} from 'react';
import {SimpleTable} from "components";
import {AppContext} from "adapter";

const TopBandwidthOrder = () => {
    const {api} = useContext(AppContext);
    return (<SimpleTable
        loadDataFun={api.top10BandwidthOrder}
        columns={[
            {
                label: '产品',
                key: 'productName'
            }, {
                label: '用户',
                key: 'purchaseUser'
            }, {
                label: '结算方式',
                key: 'balanceMethod'
            }, {
                label: '定价',
                key: 'balancePrice'
            }, {
                label: '带宽限制',
                key: 'bandwidthLimit'
            }, {
                label: '实时带宽',
                key: 'runtimeBandwidth'
            },
        ]}/>);
}

export default TopBandwidthOrder;