import React, {useContext, useEffect, useState} from 'react';
import {createUseStyles, useTheme} from "react-jss";
import {OpeDialog, SimpleTable} from "components";
import {AppContext} from "adapter";
import {Button, Grid, MenuItem, Select, TextField, Typography} from "@mui/material";
import {DeleteForever, DeviceHub, QueuePlayNext} from "@mui/icons-material";
import {withRouter} from "react-router-dom";


const useStyles = createUseStyles({
    root: {
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    tableButton: {
        marginRight: ({theme}) => theme.spacing(1)
    }, dialog: {
        width: ({theme}) => theme.spacing(60)
    },
    dialogInput: {
        width: '100%',
        marginBottom: ({theme}) => theme.spacing(2)
    },
});

const IpSourceRatioEditorDialog = (props) => {
    const {editItem, openDialog, setOpenDialog, setRefresh, productId} = props;
    const theme = useTheme();
    const classes = useStyles({theme});

    const {api} = useContext(AppContext);
    const [ipResources, setIpResources] = useState([]);

    const [sourceKey, setSourceKey] = useState(!!editItem['sourceKey'] ? editItem['sourceKey'] : '')
    const [ratio, setRatio] = useState(!!editItem['ratio'] ? editItem['ratio'] : 1)

    useEffect(() => {
        api.getAllIpSources().then(res => {
            if (res.status === 0) {
                setIpResources(res.data);
                if (!sourceKey && res.data.length > 0) {
                    setSourceKey(res.data[0].sourceKey)
                }
            }
        })
    }, [api, sourceKey])

    return (<OpeDialog
        title="添加IP资源"
        opeContent={(
            <>
                <Grid
                    container
                    spacing={6}
                    wrap="wrap"
                >
                    <Grid item xs={12}>
                        <Typography gutterBottom variant="h6">IP资源标记</Typography>
                        <Select
                            className={classes.dialogInput}
                            size="small"
                            variant="outlined"
                            value={sourceKey}
                            onChange={(e) => setSourceKey(e.target.value)}
                        >
                            {ipResources.map(item => (
                                <MenuItem key={item.sourceKey} value={item.sourceKey}>
                                    <pre>{item.sourceKey}</pre>
                                </MenuItem>
                            ))}
                        </Select>
                        <Typography gutterBottom variant="h6">比例</Typography>
                        <TextField
                            className={classes.dialogInput}
                            size="small"
                            variant="outlined"
                            value={ratio}
                            onChange={(e) => setRatio(e.target.value)}/>
                    </Grid>
                </Grid>
            </>
        )}
        openDialog={openDialog}
        setOpenDialog={setOpenDialog}
        doDialog={() => {
            return api.addSourceToProduct({
                sourceKey: sourceKey,
                productId: productId,
                ratio: ratio
            }).then(res => {
                if (res.status === 0) {
                    setRefresh(+new Date());
                    return '操作成功';
                }
                throw new Error(res.message);
            });
        }}
        okText="保存"
        okType="primary"/>);
}


const IpSourceRatio = (props) => {

    const {api} = useContext(AppContext);
    const theme = useTheme();
    const classes = useStyles({theme});

    const [openDialog, setOpenDialog] = useState(false);
    const [editItem, setEditItem] = useState({})
    const [refresh, setRefresh] = useState(+new Date())

    const loadData = () => {
        return api.listProductIpSources({productId: props.id})
    };


    return (
        <SimpleTable
            refresh={refresh}
            loadDataFun={loadData}
            actionEl={
                <>
                    <Button
                        startIcon={<QueuePlayNext/>}
                        color="primary"
                        variant="contained"
                        onClick={() => setOpenDialog(true)}
                    >
                        添加IP资源
                    </Button>
                    <IpSourceRatioEditorDialog
                        openDialog={openDialog}
                        setOpenDialog={setOpenDialog}
                        editItem={editItem}
                        setRefresh={setRefresh}
                        productId={props.id}
                    />
                </>
            }
            columns={[
                {
                    label: 'IP源',
                    key: 'sourceKey'
                }, {
                    label: '流量比例',
                    key: 'ratio'
                },
                {
                    label: '创建时间',
                    key: 'createTime'
                }, {
                    label: '操作',
                    render: (item) => (
                        <>
                            <Button
                                startIcon={<DeviceHub style={{fontSize: 16}}/>}
                                size="small"
                                color="primary"
                                className={classes.tableButton}
                                onClick={() => {
                                    setEditItem(item);
                                    setOpenDialog(true);
                                }}
                                variant="contained">编辑</Button>
                            <Button
                                startIcon={<DeleteForever style={{fontSize: 16}}/>}
                                size="small"
                                color="secondary"
                                className={classes.tableButton}
                                onClick={() => {
                                    api.deleteIpSourceFromProduct({id: item.id})
                                        .then(res => {
                                            if (res.status === 0) {
                                                setRefresh(+new Date());
                                            }
                                        });
                                }}
                                variant="contained">删除</Button>
                        </>
                    )
                }
            ]}
        />);
}

export default withRouter(IpSourceRatio);

