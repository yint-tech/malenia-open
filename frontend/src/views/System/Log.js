import React, {useContext, useState} from 'react';

import {AppContext} from "adapter";
import {BackendPagedTable} from "components";
import moment from "moment/moment";

const LogPanel = () => {
    const {api} = useContext(AppContext);
    const [searchParam, setSearchParam] = useState({
        "key": ""
    })
    return (
        <BackendPagedTable
            loadDataFun={api.logList}
            searchParam={searchParam}
            columns={[{
                label: '操作人',
                key: 'username'
            }, {
                label: '操作',
                key: 'operation'
            }, {
                label: '参数',
                key: 'params'
            }, {
                label: '时间',
                render: (item) => moment(new Date(item.createTime)).format('YYYY-MM-DD HH:mm:ss')
            },
            ]}
        />
    );
};

export default LogPanel;
