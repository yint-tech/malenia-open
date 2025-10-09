import React, {useContext, useState} from "react";
import {createUseStyles, useTheme} from "react-jss";
import {OpeDialog, SimpleTable} from "components";
import CodeMirror from "@uiw/react-codemirror";
import {AppContext} from "adapter";
import moment from "moment/moment";
import {Button, Grid, MenuItem, Select, TextField, Typography} from "@mui/material";
import {DeleteForever, DeviceHub, GetApp, MenuBook, Spellcheck} from "@mui/icons-material";
import {withRouter} from "react-router-dom";


const useStyles = createUseStyles({
    row: {
        height: "42px",
        display: "flex",
        alignItems: "center",
        marginTop: ({theme}) => theme.spacing(1),
        marginBottom: ({theme}) => theme.spacing(2)
    },
    dialogInput: {
        width: "100%",
        marginBottom: ({theme}) => theme.spacing(2)
    },
    btn: {
        marginRight: ({theme}) => theme.spacing(1)
    }
});

function MitmScript() {
    const theme = useTheme();
    const classes = useStyles({theme});
    const {api} = useContext(AppContext);

    const [editId, setEditId] = useState("");
    const [groovyText, setGroovyText] = useState("");
    const [groovyPriority, setGroovyPriority] = useState(0);
    const [groovyName, setGroovyName] = useState("");
    const [openDialog, setOpenDialog] = useState(false);

    const [refresh, setRefresh] = useState(+new Date());


    return (
        <div>
            <SimpleTable
                loadDataFun={api.listMitmScript}
                refresh={refresh}
                actionEl={(<>
                    <Button
                        startIcon={<GetApp/>}
                        color="primary"
                        className={classes.btn}
                        variant="contained"
                        onClick={() => {
                            let url = `${api.urls.downloadCaCertificate}?pem=true`;
                            window.open(url);
                        }}
                    >
                        下载ca证书
                    </Button>
                    <Button
                        startIcon={<MenuBook/>}
                        color="primary"
                        className={classes.btn}
                        variant="contained"
                        onClick={() => {
                            window.open("/malenia-doc/01_user_manual/06_mitm.html");
                        }}
                    >
                        参考文档
                    </Button>
                    <Button
                        startIcon={<Spellcheck/>}
                        color="primary"
                        variant="contained"
                        onClick={() => {
                            setOpenDialog(true);
                            setEditId("");
                            setGroovyName("");
                            setGroovyPriority(0);
                            setGroovyText("");
                        }}
                    >
                        添加 Groovy 脚本
                    </Button>
                </>)
                }
                columns={
                    [
                        {
                            label: '脚本名称',
                            key: 'name'
                        },
                        {
                            label: '优先级',
                            key: 'priority'
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
                                            startIcon={<DeviceHub style={{fontSize: 16}}/>}
                                            size="small"
                                            color="primary"
                                            className={classes.tableButton}
                                            onClick={() => {
                                                setOpenDialog(true);
                                                setEditId(item.id);
                                                setGroovyName(item.name);
                                                setGroovyPriority(item.priority);
                                                setGroovyText(item.content);
                                            }}
                                            variant="contained">编辑</Button>
                                        <Button
                                            startIcon={<DeleteForever style={{fontSize: 16}}/>}
                                            size="small"
                                            color="secondary"
                                            className={classes.tableButton}
                                            onClick={() => {
                                                api.removeMitmScript({id: item.id})
                                                    .then((res) => {
                                                        if (res.status === 0) {
                                                            setRefresh(+new Date())
                                                        }
                                                    })
                                            }}
                                            variant="contained">删除</Button>
                                    </>
                                )
                            }
                        }]
                }
            />
            <OpeDialog
                maxWidth={"lg"}
                fullWidth
                title="添加 Groovy 脚本"
                opeContent={(
                    <>
                        <Grid
                            container
                            spacing={6}
                            wrap="wrap"
                        >
                            <Grid item xs={12}>
                                <Typography gutterBottom variant="h6">名字</Typography>
                                <TextField
                                    className={classes.dialogInput}
                                    size="small"
                                    variant="outlined"
                                    value={groovyName}
                                    onChange={(e) => setGroovyName(e.target.value)}/>
                                <Typography gutterBottom variant="h6">优先级</Typography>
                                <Select
                                    className={classes.dialogInput}
                                    style={{height: "40px"}}
                                    variant="outlined"
                                    value={groovyPriority}
                                    onChange={(e) => {
                                        setGroovyPriority(e.target.value);
                                    }}
                                >
                                    {Array.from({length: 10}).map((d, index) => (
                                        <MenuItem key={d + index} value={index}>
                                            {index}
                                        </MenuItem>
                                    ))}
                                </Select>
                                <Typography gutterBottom variant="h6">内容</Typography>
                                <CodeMirror
                                    value={groovyText}
                                    height="300px"
                                    onChange={(value) => setGroovyText(value)}
                                />
                            </Grid>
                        </Grid>
                    </>
                )}
                openDialog={openDialog}
                setOpenDialog={setOpenDialog}
                doDialog={() => {
                    return api.editMitmScript({
                        id: editId || undefined,
                        name: groovyName,
                        priority: groovyPriority,
                        content: groovyText
                    }).then(res => {
                        if (res.status === 0) {
                            setRefresh(+new Date());
                            return "操作成功";
                        }
                        throw new Error(res.message);
                    });
                }}
                okText="保存"
                okType="primary"/>
        </div>);
}

export default withRouter(MitmScript);

