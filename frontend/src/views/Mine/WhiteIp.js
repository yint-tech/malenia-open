import React, {useContext, useState} from "react";
import {createUseStyles, useTheme} from "react-jss";
import {AppContext} from "adapter";
import {OpeDialog, SimpleTable} from "components";
import {Button, Grid, TextField, Typography} from "@mui/material";
import {DeleteForever, QueuePlayNext} from "@mui/icons-material";

const useStyles = createUseStyles({
    tableButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
    dialogInput: {
        width: "100%"
    }
});

const WhiteIp = () => {
    const {api} = useContext(AppContext);
    const theme = useTheme();
    const classes = useStyles({theme});

    const [ip, setIp] = useState('');
    const [desc, setDesc] = useState('');

    const [openDialog, setOpenDialog] = useState(false);

    const [refresh, setRefresh] = useState(+new Date());


    const doDelete = (item) => {
        api.deleteAuthWhiteIp({id: item.id})
            .then(res => {
                if (res.status === 0) {
                    api.successToast("删除成功");
                    setRefresh(+new Date());
                }
            });
    }


    return (<div>
        <SimpleTable
            refresh={refresh}
            loadDataFun={api.listAuthWhiteIp}
            actionEl={(
                <Button
                    startIcon={<QueuePlayNext/>}
                    color="primary"
                    variant="contained"
                    onClick={() => setOpenDialog(true)}
                >
                    添加白名单
                </Button>
            )}
            columns={[
                {
                    label: 'IP',
                    key: 'whiteIp'
                }, {
                    label: '描述',
                    key: 'comment'
                }, {
                    label: '操作',
                    render: (item) => (
                        <>
                            <Button
                                startIcon={<DeleteForever style={{fontSize: 16}}/>}
                                size="small"
                                color="secondary"
                                className={classes.tableButton}
                                onClick={() => doDelete(item)}
                                variant="contained">删除</Button>
                        </>
                    )
                }]
            }/>
        <OpeDialog
            title="添加白名单"
            opeContent={(
                <>
                    <Grid
                        container
                        spacing={6}
                        wrap="wrap"
                    >
                        <Grid item xs={12}>
                            <Typography gutterBottom variant="h6">IP</Typography>
                            <TextField
                                placeholder={"可缺省不填写"}
                                className={classes.dialogInput}
                                size="small"
                                variant="outlined"
                                value={ip}
                                onChange={(e) => setIp(e.target.value)}/>
                            <Typography gutterBottom variant="h6">
                                IP 配置支持
                                <a
                                    href="https://baike.baidu.com/item/%E6%97%A0%E7%B1%BB%E5%88%AB%E5%9F%9F%E9%97%B4%E8%B7%AF%E7%94%B1"
                                    target="_blank"
                                    rel="noopener noreferrer"> CIDR 规则</a>
                            </Typography>
                            <Typography gutterBottom variant="subtitle2">
                                eg. 208.128.0.0/11、208.130.29.0/24
                            </Typography>
                            <Typography gutterBottom variant="h6">描述</Typography>
                            <TextField
                                placeholder={"可缺省不填写"}
                                className={classes.dialogInput}
                                size="small"
                                variant="outlined"
                                value={desc}
                                onChange={(e) => setDesc(e.target.value)}/>
                        </Grid>
                    </Grid>
                </>
            )}
            openDialog={openDialog}
            setOpenDialog={setOpenDialog}
            doDialog={() => {
                return api.addWhiteIp({
                    ip: ip,
                    comment: desc
                }).then(res => {
                    if (res.status === 0) {
                        setRefresh(+new Date());
                        return '操作成功';
                    }
                });
            }}
            okText="保存"
            okType="primary"/>
    </div>)
}

export default WhiteIp;