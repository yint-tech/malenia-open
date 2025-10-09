import React, {useContext, useEffect, useState} from 'react';
import {createUseStyles, useTheme} from "react-jss";
import {AppContext} from "adapter";
import {Link, useHistory, withRouter} from "react-router-dom";
import {Loading, Table} from "components";
import {
    Alert,
    AlertTitle, Button, Card,
    CardActions, CardContent,
    FormControl,
    FormControlLabel,
    Grid,
    MenuItem, Radio,
    RadioGroup, Select, Switch, TextField,
    Typography
} from "@mui/material";

const useStyles = createUseStyles({
    root: {
        margin: ({theme}) => theme.spacing(2)
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

    content: {
        padding: 0
    },
    nameContainer: {
        display: "flex",
        alignItems: "center"
    },
    avatar: {
        marginRight: ({theme}) => theme.spacing(2)
    },

    tableButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
    dialogInput: {
        width: "100%"
    },
    mt: {
        marginTop: ({theme}) => theme.spacing(1)
    }
});


const OrderEditor = (props) => {
        const theme = useTheme();
        const classes = useStyles({theme});
        const {productId} = props;
        const {api, user} = useContext(AppContext);
        const history = useHistory();


        const [order, setOrder] = useState({
            "balanceMethod": "METHOD_HOUR",
            "connectTimeout": 1000,
            "maxFailoverCount": 4
        });
        useEffect(() => {
            api.orderDetail({
                productId
            }).then((res) => {
                if (res.status === 0) {
                    setOrder(res.data)
                } else {
                    history.push("/not-found")
                }
            })
        }, [api, history, productId]);

        const doEditOrder = () => {
            return api.updateOrder({
                ...order
            }).then(res => {
                if (res.status === 0) {
                    return "操作成功";
                }
                throw new Error(res.message);
            });
        };


        return (
            <Card className={classes.root}>
                <CardContent>
                    {
                        order.id ? (
                            <>

                                <Grid
                                    container
                                    spacing={4}
                                    wrap="wrap"
                                >
                                    <Grid item xs={12}>
                                        {!user.authAccount ? (
                                            <Alert severity="warning">
                                                <AlertTitle>紧急！！！</AlertTitle>
                                                建议先设置代理账号密码，代理账密和后台账密相同容易导致您的接入密码被硬编码到代码中从而泄漏后台账户 <Link
                                                to="/mine"><strong>去设置</strong></Link>
                                            </Alert>
                                        ) : (
                                            <>
                                                <Typography gutterBottom variant="subtitle2">
                                                    代理链接使用如下 <a
                                                    href="https://baike.baidu.com/item/curl/10098606?fr=aladdin"
                                                    target="_blank"
                                                    rel="noopener noreferrer">curl</a> . eg.
                                                </Typography>
                                                <code>
                                                    curl
                                                    -x {user.authAccount}:{user.authPwd}@{window.location.hostname}:{order.mappingPortSpace && order.mappingPortSpace.split("-")[0]} https://www.baidu.com/
                                                </code>
                                                {(order.tuningParam && order.tuningParam.items && order.tuningParam.items.length) ? (
                                                    <>
                                                        <Typography gutterBottom variant="h6" style={{marginTop: 10}}>
                                                            隧道路由参数
                                                        </Typography>
                                                        <Table
                                                            style={{height: "auto"}}
                                                            size="small"
                                                            data={order.tuningParam.items}
                                                            columns={[{
                                                                label: "参数",
                                                                key: "param"
                                                            }, {
                                                                label: "是否可为空",
                                                                render: (item) => item.nullable ? "是" : "否"
                                                            }, {
                                                                label: "描述",
                                                                key: "description"
                                                            }, {
                                                                label: "参数值",
                                                                render: (item) => item.enums.join(",")
                                                            }]}
                                                        />
                                                    </>
                                                ) : null}
                                            </>
                                        )}
                                    </Grid>
                                    <Grid item xs={12}>
                                        <Typography className={classes.mt} variant="h6">
                                            计费方式
                                        </Typography>
                                        <FormControl component="fieldset">
                                            <RadioGroup row value={order.balanceMethod}
                                                        onChange={(e) => setOrder({
                                                            ...order,
                                                            balanceMethod: e.target.value
                                                        })}>
                                                <FormControlLabel value="METHOD_HOUR" control={<Radio/>}
                                                                  label="按小时计费"/>
                                                <FormControlLabel value="METHOD_FLOW" control={<Radio/>}
                                                                  label="按流量计费(0.01G)"/>
                                            </RadioGroup>
                                        </FormControl>
                                    </Grid>
                                    <Grid item xs={12}>
                                        <Typography variant="h6">
                                            代理连接超时（毫秒）
                                        </Typography>
                                        <TextField
                                            className={classes.dialogInput}
                                            size="small"
                                            variant="outlined"
                                            type="number"
                                            placeholder="请输入代理连接"
                                            value={order.connectTimeout}
                                            onChange={(e) => setOrder({...order, connectTimeout: e.target.value})}
                                        />
                                    </Grid>
                                    <Grid item xs={12}>
                                        <Typography gutterBottom variant="h6">failover次数</Typography>
                                        <Select
                                            className={classes.dialogInput}
                                            style={{height: "40px"}}
                                            variant="outlined"
                                            value={order.maxFailoverCount}
                                            onChange={(e) => setOrder({...order, maxFailoverCount: e.target.value})}
                                        >
                                            {Array.from({length: 10}).map((d, index) => (
                                                <MenuItem key={d + index} value={index + 1}>
                                                    {index + 1}
                                                </MenuItem>
                                            ))}
                                        </Select>

                                    </Grid>
                                    <Grid item xs={12}>
                                        <Typography gutterBottom variant="h6">随机隧道</Typography>
                                        <Switch color="primary" checked={order.randomTurning} onChange={(e) => setOrder(
                                            {...order, randomTurning: e.target.checked})}/>
                                    </Grid>
                                </Grid>
                            </>
                        ) : <Loading/>
                    }

                </CardContent>
                <CardActions className={classes.actions}>
                    <Button className={classes.btns} variant="contained" color="primary" onClick={doEditOrder}>保存</Button>
                </CardActions>
            </Card>
        );
    }
;

export default withRouter(OrderEditor);
