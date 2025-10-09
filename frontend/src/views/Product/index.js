import React, {useContext, useState} from 'react';
import {createUseStyles, useTheme} from "react-jss";

import {OpeDialog, SimpleTable} from 'components';
import {AppContext} from 'adapter';
import {useHistory, withRouter} from "react-router-dom";
import {Button, Card, CardActions, Grid, Switch, Typography} from "@mui/material";
import {AddPhotoAlternate, DeviceHub} from "@mui/icons-material";

const useStyles = createUseStyles({
    root: {
        flexGrow: 1,
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    tableButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
});

const ProxyList = () => {
    const theme = useTheme();
    const classes = useStyles({theme});
    const {api} = useContext(AppContext);
    const history = useHistory();
    const [operateItem, setOperateItem] = useState({});
    const [openStopProductDialog, setOpenStopProductDialog] = useState(false);
    const [refresh, setRefresh] = useState(+new Date());


    const updateProductStatus = (enabled) => {
        api.updateProductStatus({productId: operateItem.productId, enabled: enabled})
            .then(res => {
                if (res.status === 0) {
                    setRefresh(+new Date());
                }
            })
    }


    return (
        <div className={classes.root}>
            <Card className={classes.content}>

                <SimpleTable
                    actionEl={
                        (<Button
                            startIcon={<AddPhotoAlternate/>}
                            color="primary"
                            variant="contained"
                            onClick={() => history.push('/createProduct')}
                        >
                            创建产品
                        </Button>)}
                    refresh={refresh}
                    loadDataFun={api.listAllProducts}
                    columns={[
                        {
                            label: '产品名',
                            key: 'productName'
                        }, {
                            label: '产品描述',
                            key: 'description'
                        }
                        , {
                            label: '端口策略',
                            key: 'mappingPortSpace'
                        }, {
                            label: '计时价格',
                            render: (item) => {
                                return `${item.hourPrice}元/小时`;
                            }
                        }, {
                            label: '计量价格',
                            render: (item) => {
                                return `${item.flowPrice}元/0.01G`;
                            }
                        }, {
                            label: '流量计价',
                            key: 'flowPrice'
                        }
                        , {
                            label: '操作',
                            render: (item) => (
                                <>
                                    <Button
                                        startIcon={<DeviceHub style={{fontSize: 16}}/>}
                                        size="small"
                                        color="primary"
                                        className={classes.tableButton}
                                        onClick={() => history.push('/ProductWorkSpace/' + item.productId)}
                                        variant="contained">详情</Button>
                                    <Switch
                                        checked={item.enabled}
                                        onChange={() => {
                                            setOperateItem(item);
                                            if (item.enabled) {
                                                setOpenStopProductDialog(true)
                                            } else {
                                                updateProductStatus(true);
                                            }
                                        }}
                                        color="primary"
                                        inputProps={{'aria-label': 'primary checkbox'}}
                                    />
                                </>
                            )
                        }
                    ]}
                />
                <CardActions>
                    <OpeDialog
                        title={"确定停用该服务？"}
                        openDialog={openStopProductDialog}
                        setOpenDialog={setOpenStopProductDialog}
                        doDialog={() => {
                            updateProductStatus(false)
                        }}
                        opeContent={(<Grid>
                            <Typography gutterBottom variant="h6">
                                产品名：{operateItem.productName}
                            </Typography>
                            <Typography gutterBottom variant="h6">
                                产品描述：{operateItem.description}
                            </Typography>
                        </Grid>)}
                    />
                </CardActions>


            </Card>
        </div>
    );
};
export default withRouter(ProxyList);
