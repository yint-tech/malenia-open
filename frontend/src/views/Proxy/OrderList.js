import React, {useCallback, useContext, useState} from "react";
import {createUseStyles, useTheme} from "react-jss";
import {AppContext} from "adapter";
import {OpeDialog, SimpleTable} from "components";
import {useHistory, withRouter} from "react-router-dom";
import {Button, Grid, Switch, TextField, Typography} from "@mui/material";
import {Edit} from "@mui/icons-material";

const useStyles = createUseStyles({
    root: {},
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
    actions: {
        paddingTop: ({theme}) => theme.spacing(2),
        paddingBottom: ({theme}) => theme.spacing(2),
        justifyContent: "center"
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

const OrderList = () => {
    const {api, user} = useContext(AppContext);
    const theme = useTheme();
    const classes = useStyles({theme});
    const history = useHistory();

    const [rePriceItem, setRePriceItem] = useState({
        "orderId": "",
        "newOrderPrice": 10,
        "bandwidthLimit": 10
    });
    const [rePriceDialog, setRePriceDialog] = useState(false);

    const [refresh, setRefresh] = useState(+new Date());

    const loadDataFun = useCallback(() => {
        return new Promise((resolve, reject) => {
            Promise.all([
                api.listOrder(),
                api.listAllProducts()
            ]).then(res => {
                if (res[0].status === 0 && res[1].status === 0) {
                    let products = {};
                    res[1].data.map(item => {
                        products[item.productId] = item;
                        return item;
                    });
                    let orders = res[0].data;
                    orders = orders.map(item => {
                        return {
                            ...products[item.productId],
                            ...item,
                        }
                    });
                    resolve({
                        data: orders,
                        status: 0
                    })
                } else {
                    reject(res[0].message || res[1].message)
                }
            }).catch(e => {
                reject(e)
            })
        });

    }, [api]);

    const doEditOrder = (order) => {
        return api.updateOrder({
            ...order
        }).then(res => {
            if (res.status === 0) {
                setRefresh(+new Date());
                return "操作成功";
            }
            throw new Error(res.message);
        });
    };


    return (<div>
        <SimpleTable
            refresh={refresh}
            loadDataFun={loadDataFun}
            columns={[
                {
                    label: "产品名",
                    key: "productName"
                }, {
                    label: "价格",
                    key: "balancePrice"
                }, {
                    label: "带宽限制",
                    render: (item) => {
                        if (item.balanceMethod === "METHOD_HOUR") {
                            return item.bandwidthLimit + "m";
                        }
                        return "-";
                    }
                }, {
                    label: "代理端口",
                    key: "mappingPortSpace"
                }, {
                    label: "随机隧道",
                    render: (item) => {
                        return item.randomTurning ? "是" : "否";
                    }
                }, {
                    label: "failover次数",
                    key: "maxFailoverCount"
                }, {
                    label: "代理连接超时(毫秒)",
                    key: "connectTimeout"
                }, {
                    label: "计费方式",
                    render: (item) => {
                        return {
                            "METHOD_HOUR": "按小时",
                            "METHOD_FLOW": "按流量(0.01G)"
                        }[item.balanceMethod];
                    }
                }, {
                    label: "启用",
                    render: (item) => {
                        return item.enabled ? "是" : "否"
                    }
                },
                {
                    label: "操作",
                    render: (item) => (
                        <>
                            <Button
                                startIcon={<Edit style={{fontSize: 16}}/>}
                                size="small"
                                color="primary"
                                className={classes.tableButton}
                                onClick={() => {
                                    history.push("/orderDetail/" + item.productId)
                                }}
                                variant="contained">详情</Button>


                            {(user.mock && item.balanceMethod === "METHOD_HOUR") ? (
                                <Button
                                    startIcon={<Edit style={{fontSize: 16}}></Edit>}
                                    size="small"
                                    color="primary"
                                    className={classes.tableButton}
                                    onClick={() => {
                                        setRePriceItem({
                                            orderId: item.id,
                                            bandwidthLimit: item.bandwidthLimit,
                                            newOrderPrice: item.balancePrice
                                        });
                                        setRePriceDialog(true);
                                    }}
                                    variant="contained">修改价格 | 带宽</Button>
                            ) : null}
                            <Switch
                                checked={item.enabled}
                                onChange={() => {
                                    // item.enabled = !item.enabled;
                                    doEditOrder({
                                        ...item,
                                        enabled: !item.enabled
                                    });
                                }}
                                color="primary"
                                inputProps={{"aria-label": "primary checkbox"}}
                            />
                        </>
                    )
                }]
            }/>
        <OpeDialog
            title="修改带宽 | 价格"
            opeContent={(
                <>
                    <Grid
                        container
                        spacing={6}
                        wrap="wrap"
                    >
                        <Grid item xs={12}>
                            <Typography variant="h6">
                                价格
                            </Typography>
                            <TextField
                                className={classes.dialogInput}
                                size="small"
                                variant="outlined"
                                value={rePriceItem.newOrderPrice}
                                onChange={(e) => setRePriceItem({
                                    ...rePriceItem,
                                    newOrderPrice: e.target.value
                                })}/>
                            <Typography className={classes.mt} variant="h6">
                                带宽
                            </Typography>
                            <TextField
                                className={classes.dialogInput}
                                size="small"
                                variant="outlined"
                                value={rePriceItem.bandwidthLimit}
                                onChange={(e) => setRePriceItem({
                                    ...rePriceItem,
                                    bandwidthLimit: e.target.value
                                })}/>
                        </Grid>
                    </Grid>
                </>
            )}
            openDialog={rePriceDialog}
            setOpenDialog={setRePriceDialog}
            doDialog={() => {
                return api.setOrderBandwidthLimit(rePriceItem).then(res => {
                    if (res.status === 0) {
                        setRefresh(+new Date());
                        return "操作成功";
                    }
                    throw new Error(res.message);
                });
            }}
            okText="保存"
            okType="primary"
        />
    </div>)
}

export default withRouter(OrderList);