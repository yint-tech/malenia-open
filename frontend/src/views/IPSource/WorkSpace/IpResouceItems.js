import React, {useContext} from "react";
import {SimpleTable} from "components";
import {AppContext} from "adapter";


const IpResourceItems = (props) => {

    const {api} = useContext(AppContext);
    return (<SimpleTable
        loadDataFun={() => {
            return api.getResourceContentList({
                ipSourceKey: props.ipSourceKey
            })
        }}
        columns={[
            {
                label: 'Id',
                key: 'resourceId'
            },
            {
                label: '用户名',
                key: 'userName'
            },
            {
                label: '密码',
                key: 'password'
            },
            {
                label: '过期时间',
                key: 'expireTime'
            },
            {
                label: '出口ip',
                key: 'outIp'
            }, {
                label: '出口城市',
                render: (item) => {
                    let cityResponse = item['cityResponse'];
                    if (!cityResponse) {
                        return "未知";
                    }
                    let fetch = (node) => {
                        let names = node['names'];
                        return Object.values(names).length === 0 ? "" : names['zh-CN'] ?
                            names['zh-CN'] :
                            Object.values(names)[0];
                    }
                    let country = fetch(cityResponse['country']);
                    let city = fetch(cityResponse['city']);
                    return (country ? country : "") + (city ? "-" + city : "");
                }
            },
            {
                label: '联通测试耗时',
                key: 'testCost'
            },
            {
                label: 'online',
                render: (item) => {
                    return item['online'] ? "在线" : "预备"
                }
            }, {
                label: '总使用次数',
                key: 'totalCount'
            }, {
                label: '首次活跃',
                key: 'fistActive'
            }, {
                label: '总流量',
                key: 'totalFlow'
            }, {
                label: '缓存连接量',
                key: 'cacheConnSize'
            }
        ]}
    />);
}

export default IpResourceItems;