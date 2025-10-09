import React, {useContext, useRef, useState} from 'react';
import {createUseStyles, useTheme} from "react-jss";
import {OpeDialog, SimpleTable} from 'components';
import {AppContext} from "adapter";
import moment from "moment/moment";
import config from "config";
import {Button, Grid, Input, TextField, Typography} from "@mui/material";
import {withRouter} from "react-router-dom";
import {CloudUpload, DeleteForever, GetApp} from "@mui/icons-material";

const useStyles = createUseStyles({
    row: {
        height: '42px',
        display: 'flex',
        alignItems: 'center',
        marginTop: ({theme}) => theme.spacing(1),
        marginBottom: ({theme}) => theme.spacing(2)
    },
    dialogInput: {
        width: '100%',
        marginBottom: ({theme}) => theme.spacing(2)
    },
    tableButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
});

function FileAssets() {
    const theme = useTheme();
    const classes = useStyles({theme});
    const {api, user} = useContext(AppContext);

    const fileRef = useRef();
    const [filePath, setFilePath] = useState('');
    const [openDialog, setOpenDialog] = useState(false);

    const [refresh, setRefresh] = useState(+new Date())

    return (
        <div>
            <SimpleTable
                refresh={refresh}
                loadDataFun={api.listAsset}
                actionEl={(
                    <Button
                        startIcon={<CloudUpload/>}
                        color="primary"
                        variant="contained"
                        onClick={() => setOpenDialog(true)}
                    >
                        添加文件
                    </Button>
                )}
                columns={
                    [
                        {
                            label: '操作人',
                            key: 'user'
                        },
                        {
                            label: '文件路径',
                            key: 'path'
                        },
                        {
                            label: '更新时间',
                            render: (item) => moment(new Date(item.updateTime)).format('YYYY-MM-DD HH:mm:ss')
                        },
                        {
                            label: '操作',
                            render: (item) => {
                                return (
                                    <>
                                        <Button
                                            startIcon={<GetApp style={{fontSize: 16}}/>}
                                            size="small"
                                            color="primary"
                                            className={classes.tableButton}
                                            onClick={() => {
                                                let url = `${api.urls.downloadAsset}?${config.login_token_key}=${user.loginToken}&id=${item.id}`;
                                                window.open(url)
                                            }}
                                            variant="contained">下载</Button>
                                        <Button
                                            startIcon={<DeleteForever style={{fontSize: 16}}/>}
                                            size="small"
                                            color="secondary"
                                            className={classes.tableButton}
                                            onClick={() => {
                                                api
                                                    .deleteAsset({id: item.id})
                                                    .then(res => {
                                                        if (res.status === 0) {
                                                            setRefresh(+new Date())
                                                        }
                                                    });
                                            }}
                                            variant="contained">删除</Button>
                                    </>
                                )
                            }
                        },]

                }/>
            <OpeDialog
                title="添加文件"
                opeContent={(
                    <>
                        <Grid
                            container
                            spacing={6}
                            wrap="wrap"
                        >
                            <Grid item xs={12}>
                                <Typography gutterBottom variant="h6">文件</Typography>
                                <Input
                                    className={classes.dialogInput}
                                    inputRef={fileRef}
                                    size="small"
                                    type="file"
                                    variant="outlined"
                                    onChange={(e) => {
                                        setFilePath('/' + fileRef.current.files[0].name)
                                    }}/>
                                <Typography gutterBottom variant="h6">文件路径</Typography>
                                <TextField
                                    className={classes.dialogInput}
                                    size="small"
                                    variant="outlined"
                                    value={filePath}
                                    onChange={(e) => setFilePath(e.target.value)}/>
                            </Grid>
                        </Grid>
                    </>
                )}
                openDialog={openDialog}
                setOpenDialog={setOpenDialog}
                doDialog={() => {
                    return api.uploadAsset({
                        path: filePath,
                        file: fileRef.current.files[0]
                    }).then(res => {
                        if (res.status === 0) {
                            setRefresh(+new Date());
                            return '操作成功';
                        }
                        throw new Error(res.message);
                    });
                }}
                okText="保存"
                okType="primary"/>
        </div>
    );
}

export default withRouter(FileAssets);