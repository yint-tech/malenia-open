import React, {useContext, useEffect, useState} from 'react';
import {createUseStyles, useTheme} from "react-jss";
import {useHistory, useParams, withRouter} from 'react-router-dom';
import {AppContext} from "adapter";
import {OpeDialog} from "components";
import CodeMirror from "@uiw/react-codemirror";
import {
    Button, Card,
    CardActions,
    CardContent,
    CardHeader,
    Checkbox,
    Divider, FormControl,
    Grid,
    Switch,
    TextField,
    Typography
} from "@mui/material";


const useStyles = createUseStyles({
    root: {
        margin: ({theme}) => theme.spacing(4)
    },
    inputItem: {
        width: '100%'
    },
    actions: {
        justifyContent: 'center',
        padding: ({theme}) => theme.spacing(4),
    },
    btns: {
        paddingLeft: ({theme}) => theme.spacing(4),
        paddingRight: ({theme}) => theme.spacing(4),
        marginLeft: ({theme}) => theme.spacing(2),
        marginRight: ({theme}) => theme.spacing(2),
    },
    noMaxWidth: {
        maxWidth: 'none',
    },
    customIpResourceHandler: {
        overflow: "hidden",
        whiteSpace: "nowrap",
        textOverflow: "ellipsis"
    },
    supportProtocol: {
        marginTop: ({theme}) => theme.spacing(1),
        display: 'flex',
        alignItems: 'center'
    },
});

function InputItem({col = 6, label = '', placeholder = '', value, onChange, disabled = false}) {
    const theme = useTheme();
    const classes = useStyles({theme});
    return (
        <Grid item xs={col}>
            <Typography
                gutterBottom
                variant="h6"
            >{label}</Typography>
            <TextField
                disabled={disabled}
                className={classes.inputItem}
                size="small"
                variant="outlined"
                placeholder={placeholder}
                value={value}
                onChange={(e) => onChange(e)}/>
        </Grid>
    )
}

const IPSourceEditor = (props) => {
    const history = useHistory();
    const params = useParams();
    const {api} = useContext(AppContext);
    const targetSource = props.ipSourceKey || params.ipSourceKey;

    const theme = useTheme();
    const classes = useStyles({theme});
    const [form, setForm] = useState(() => {
        return {
            sourceKey: '',
            description: '',
            loadResourceHandler: '',
            loadUrl: '',
            authUserNameExp: '',
            upUserName: '',
            upUserPassword: '',
            poolSize: 10,
            connIdleSeconds: '20',
            enabled: true,
            makeConnInterval: '20',
            maxAlive: '300',
            needTest: false,
            reloadInterval: '240',
            supportProtocol: 'HTTP,HTTPS',
        }
    });

    const [showCustomIpResourceHandlerDialog, setShowCustomIpResourceHandlerDialog] = useState(false);

    const [supportHTTP, setSupportHTTP] = useState(false);
    const [supportHTTPS, setSupportHTTPS] = useState(true);
    const [supportSocks5, setSupportSocks5] = useState(true);

    const [editModel, setEditModel] = useState(false);


    useEffect(() => {
        if (!targetSource) {
            setEditModel(false);
            return;
        }
        api.getIpSourceDetail({ipSourceKey: targetSource}).then(res => {
            if (res.status === 0) {
                let pp = res.data;

                for (let i in pp) {
                    pp[i] = pp[i] || '';
                }

                let supportProtocols = pp.supportProtocol.split(',');
                setSupportHTTP(supportProtocols.indexOf("HTTP") >= 0)
                setSupportHTTPS(supportProtocols.indexOf("HTTPS") >= 0)
                setSupportSocks5(supportProtocols.indexOf("SOCKS5") >= 0)
                setForm({...pp})
                setEditModel(true)
            } else {
                setEditModel(false)
            }
        })
    }, [api, targetSource]);


    const doSave = () => {
        let saveIpSource = {...form};
        let protocols = "";
        if (supportHTTP) {
            protocols += ",HTTP";
        }
        if (supportHTTPS) {
            protocols += ",HTTPS";
        }
        if (supportSocks5) {
            protocols += ",SOCKS5";
        }
        saveIpSource.supportProtocol = protocols;

        api.editIpSource(saveIpSource).then(res => {
            if (res.status === 0) {
                history.go(-1);
            }
        })
    }

    return (
        <Card className={classes.root}>
            <CardHeader title={editModel ? "编辑IP资源" : "新增IP资源"}/>
            <Divider/>
            <CardContent>
                <Grid container spacing={6} wrap="wrap">
                    <InputItem
                        label="资源ID"
                        placeholder="请填写资源唯一标识（建议字母下划线）"
                        value={form.sourceKey}
                        disabled={editModel}
                        onChange={(e) => setForm({
                            ...form,
                            sourceKey: e.target.value
                        })}/>
                    <InputItem
                        col={6}
                        label="资源描述"
                        placeholder="请填写资源描述"
                        value={form.description}
                        onChange={(e) => setForm({
                            ...form,
                            description: e.target.value
                        })}/>
                    <Grid item xs={12}>
                        <FormControl component="fieldset">
                            <Typography
                                gutterBottom
                                variant="h6"
                            >资源下载 URL</Typography>

                        </FormControl>
                        <TextField
                            className={classes.inputItem}
                            size="small"
                            variant="outlined"
                            placeholder="ip下载连接"
                            value={form.loadUrl}
                            onChange={(e) => setForm({
                                ...form,
                                loadUrl: e.target.value
                            })}/>
                    </Grid>
                    <InputItem
                        label="资源池大小"
                        placeholder="数字"
                        value={form.poolSize}
                        onChange={(e) => setForm({
                            ...form,
                            poolSize: e.target.value
                        })}/>
                    <InputItem
                        label="鉴权账户表达式(不推荐)"
                        placeholder="请输入表达式，eg. "
                        value={form.authUserNameExpression}
                        onChange={(e) => setForm({
                            ...form,
                            authUserNameExpression: e.target.value
                        })}/>
                    <InputItem
                        label="上游代理鉴权账户"
                        placeholder="请输入上游代理鉴权账户，eg. "
                        value={form.upUserName}
                        onChange={(e) => setForm({
                            ...form,
                            upUserName: e.target.value
                        })}/>
                    <InputItem
                        label="上游代理鉴权密码"
                        placeholder="请输入上游代理鉴权密码，eg. "
                        value={form.upUserPassword}
                        onChange={(e) => setForm({
                            ...form,
                            upUserPassword: e.target.value
                        })}/>
                    <Grid item xs={6}>
                        <Typography
                            gutterBottom
                            variant="h6"
                        >当前资源是否生效</Typography>
                        <Switch
                            checked={form.enabled}
                            onChange={(e) => setForm({
                                ...form,
                                enabled: e.target.checked
                            })}
                            inputProps={{'aria-label': 'secondary checkbox'}}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <Typography
                            gutterBottom
                            variant="h6"
                        >是否需要探测可用性</Typography>
                        <Switch
                            checked={!!form.needTest}
                            onChange={(e) => setForm({
                                ...form,
                                needTest: e.target.checked
                            })}
                            inputProps={{'aria-label': 'secondary checkbox'}}
                        />
                    </Grid>
                    <InputItem
                        label="连接池连接空转时间（秒）"
                        placeholder="请输入连接池连接空转时间（秒）"
                        value={form.connIdleSeconds}
                        onChange={(e) => setForm({
                            ...form,
                            connIdleSeconds: e.target.value
                        })}/>
                    <InputItem
                        label="链接缓存池检查和创建时间间隔（秒）"
                        placeholder="请输入链接缓存池检查和创建时间间隔（秒）"
                        value={form.makeConnInterval}
                        onChange={(e) => setForm({
                            ...form,
                            makeConnInterval: e.target.value
                        })}/>

                    <InputItem
                        label="IP下载时间间隔（秒）"
                        placeholder="请输入IP下载时间间隔（秒）"
                        value={form.reloadInterval}
                        onChange={(e) => setForm({
                            ...form,
                            reloadInterval: e.target.value
                        })}/>

                    <InputItem
                        label="最长存活时间（秒）"
                        placeholder="请输入最长存活时间（秒）"
                        value={form.maxAlive}
                        onChange={(e) => setForm({
                            ...form,
                            maxAlive: e.target.value
                        })}/>
                    <Grid item xs={6}>
                        <Typography
                            gutterBottom
                            variant="h6"
                        >支持协议(建议至少要求支持Socks5)</Typography>
                        <div className={classes.supportProtocol}>
                            <Typography
                                color="textSecondary"
                                variant="body1"
                            >HTTP</Typography>
                            <Checkbox
                                checked={supportHTTP}
                                color="primary"
                                onChange={(e) => setSupportHTTP(e.target.checked)}
                            />

                            <Typography
                                color="textSecondary"
                                variant="body1"
                            >HTTPS</Typography>
                            <Checkbox checked={supportHTTPS}
                                      color="primary"
                                      onChange={(e) => setSupportHTTPS(e.target.checked)}
                            />

                            <Typography
                                color="textSecondary"
                                variant="body1"
                            >SOCKS5</Typography>
                            <Checkbox checked={supportSocks5}
                                      color="primary"
                                      onChange={(e) => setSupportSocks5(e.target.checked)}
                            />
                        </div>

                    </Grid>
                </Grid>
            </CardContent>
            <CardActions className={classes.actions}>
                <Button className={classes.btns} variant="contained" color="secondary"
                        onClick={() => history.go(-1)}>退出</Button>
                <Button className={classes.btns} variant="contained" color="success"
                        onClick={() => setShowCustomIpResourceHandlerDialog(true)}>扩展脚本</Button>
                <Button className={classes.btns} variant="contained" color="primary" onClick={doSave}>保存</Button>
                <OpeDialog title={"录入扩展脚本"}
                           maxWidth={"lg"}
                           fullWidth={true}
                           openDialog={showCustomIpResourceHandlerDialog}
                           setOpenDialog={setShowCustomIpResourceHandlerDialog}
                           opeContent={(<CodeMirror
                               height="400px"
                               value={form.loadResourceHandler}
                               onChange={(value) => {
                                   setForm({...form, loadResourceHandler: value})
                               }}
                           />)}
                />
            </CardActions>
        </Card>
    );
};

export default withRouter(IPSourceEditor);