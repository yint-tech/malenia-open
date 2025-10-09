import React, {useContext, useState} from 'react';

import {AppContext} from "adapter";
import {BackendPagedTable, SearchInput} from "components";
import {Switch} from "@mui/material";
import {createUseStyles, useTheme} from "react-jss";


const useStyles = createUseStyles({
    root: {},
    row: {
        height: '42px',
        display: 'flex',
        alignItems: 'center',
        marginTop: ({theme}) => theme.spacing(1)
    },
    spacer: {
        flexGrow: 1
    },
    searchInput: {
        marginBottom: ({theme}) => theme.spacing(2)
    },
});


const SeverNodeList = () => {

    const theme = useTheme();
    const classes = useStyles({theme});

    const {api} = useContext(AppContext);
    const [searchParam, setSearchParam] = useState({
        "key": ""
    })
    const [refresh, setRefresh] = useState(+new Date());

    const handleChange = (item) => {
        api.setServerStatus({id: item.id, enable: !item.enable})
            .then(res => {
                if (res.status === 0) {
                    api.successToast("操作成功");
                    setRefresh(+new Date());
                }
            })
    }

    return (
        <BackendPagedTable
            toolbar={
                <>
                    <SearchInput
                        className={classes.searchInput}
                        onChange={(v) => {
                            setSearchParam({
                                ...searchParam,
                                key: v
                            })
                        }}
                        placeholder="请输入关键词进行查询"
                    />
                    <span className={classes.spacer}/>
                </>
            }
            loadDataFun={api.listServer}
            searchParam={searchParam}
            setSearchParam={setSearchParam}
            refresh={refresh}
            columns={[
                {
                    label: 'ID',
                    key: 'id'
                }, {
                    label: '节点ID',
                    key: 'serverId'
                }, {
                    label: '出口',
                    key: 'outIp'
                }, {
                    label: 'web端口',
                    render: (item) => item.port || '-'
                }, {
                    label: '心跳时间',
                    key: 'lastActiveTime'
                }, {
                    label: '',
                    render: (item) => (
                        <>
                            <Switch
                                checked={item.enable}
                                onChange={() => handleChange(item)}
                                color="primary"
                                inputProps={{'aria-label': 'primary checkbox'}}
                            />
                        </>
                    )
                }
            ]}
        />
    );
};

export default SeverNodeList;
