import React, {useState} from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import {createUseStyles, useTheme} from "react-jss";
import {OpeDialog, Table} from 'components';

import apis from 'apis';
import {
    Button,
    Card,
    CardActions,
    CardContent,
    Divider,
    FormControl,
    FormControlLabel,
    Grid,
    Pagination,
    Radio,
    RadioGroup,
    TextField,
    Typography
} from "@mui/material";
import {AddShoppingCart} from "@mui/icons-material";
import {withRouter} from "react-router-dom";

const useStyles = createUseStyles({
    root: {},
    content: {
        padding: 0
    },
    nameContainer: {
        display: 'flex',
        alignItems: 'center'
    },
    avatar: {
        marginRight: ({theme}) => theme.spacing(2)
    },
    actions: {
        paddingTop: ({theme}) => theme.spacing(2),
        paddingBottom: ({theme}) => theme.spacing(2),
        justifyContent: 'center'
    },
    tableButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
    mt: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    inputItem: {
        width: '100%'
    }
});

const DataTable = props => {
    const {className, data, total, rowsPerPage, pageState, setRefresh, ...rest} = props;
    const [page, setPage] = pageState;

    const [openPurchaseDialog, setOpenPurchaseDialog] = useState(false);
    const [operateItem, setOperateItem] = useState({});


    const [balanceMethod, setBalanceMethod] = useState('METHOD_HOUR');
    const [referrer, setReferrer] = useState('');
    const [showReferrer, setShowReferrer] = useState(false);

    const theme = useTheme();
    const classes = useStyles({theme});

    const handlePageChange = (event, page) => {
        setPage(page);
    };


    return (
        <Card
            {...rest}
            className={clsx(classes.root, className)}
        >
            <CardContent className={classes.content}>
                <Table
                    collapse={true}
                    renderCollapse={(item) => (
                        <Grid
                            container
                            spacing={6}
                            wrap="wrap"
                        >
                            <Grid item xs={12}>
                                <Typography gutterBottom variant="h6">
                                    产品名：{item.productName}
                                </Typography>
                                <Typography gutterBottom variant="h6">
                                    产品描述：{item.description}
                                </Typography>
                                <Typography gutterBottom variant="h6">
                                    端口策略：{item.mappingPortSpace}
                                </Typography>
                                <Typography gutterBottom variant="h6">
                                    产品特征标签：{item.features}
                                </Typography>
                                {item.tuningParam && item.tuningParam.items && item.tuningParam.items.length && (
                                    <>
                                        <Typography gutterBottom variant="h6">
                                            隧道路由参数
                                        </Typography>
                                        <Table
                                            style={{height: 'auto', marginBottom: 10}}
                                            size="small"
                                            data={item.tuningParam.items}
                                            columns={[{
                                                label: '参数',
                                                key: 'param'
                                            }, {
                                                label: '是否可为空',
                                                render: (item) => item.nullable ? '是' : '否'
                                            }, {
                                                label: '描述',
                                                key: 'description'
                                            }, {
                                                label: '参数值',
                                                render: (item) => item.enums.join(',')
                                            }]}
                                        />
                                    </>
                                )}
                            </Grid>
                        </Grid>
                    )}
                    data={data}
                    columns={[
                        {
                            label: '产品名',
                            key: 'productName'
                        }, {
                            label: '产品描述',
                            key: 'description'
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
                            label: '操作',
                            render: (item) => (
                                <>
                                    {props.buys.includes(item.productId) ? (
                                        <Button
                                            startIcon={<AddShoppingCart style={{fontSize: 16}}/>}
                                            size="small"
                                            color="primary"
                                            disabled
                                            className={classes.tableButton}
                                            variant="contained">已购</Button>
                                    ) : (
                                        <Button
                                            startIcon={<AddShoppingCart style={{fontSize: 16}}/>}
                                            size="small"
                                            color="primary"
                                            className={classes.tableButton}
                                            onClick={() => {
                                                setOperateItem(item);
                                                setOpenPurchaseDialog(true)
                                            }}
                                            variant="contained">选购</Button>
                                    )}
                                </>
                            )
                        }
                    ]}
                />
            </CardContent>
            <CardActions className={classes.actions}>
                <Pagination
                    count={Math.ceil(total / rowsPerPage) || 1}
                    page={page}
                    onChange={handlePageChange}
                    shape="rounded"/>
            </CardActions>
            <OpeDialog
                title="确认选购产品"
                opeContent={(
                    <>
                        <Grid
                            container
                            spacing={6}
                            wrap="wrap"
                        >
                            <Grid item xs={12}>
                                <Typography gutterBottom variant="h6">
                                    产品名：{operateItem.productName}
                                </Typography>
                                <Divider/>
                                <Typography className={classes.mt} variant="h6">
                                    计费方式
                                </Typography>
                                <FormControl component="fieldset">
                                    <RadioGroup row value={balanceMethod}
                                                onChange={(e) => setBalanceMethod(e.target.value)}>
                                        <FormControlLabel value="METHOD_HOUR" control={<Radio/>} label="按小时计费"/>
                                        <FormControlLabel value="METHOD_FLOW" control={<Radio/>}
                                                          label="按流量计费(0.01G)"/>
                                    </RadioGroup>
                                </FormControl>
                                <Typography gutterBottom variant="h6">
                                    价格：{balanceMethod === 'METHOD_HOUR' ? operateItem.hourPrice + " RMB" : operateItem.flowPrice + " RMB"}
                                </Typography>
                                <Typography gutterBottom variant="h6">
                                    代理 url：{operateItem.loadUrl}
                                </Typography>
                                <Divider/>
                                <Grid container spacing={6}>
                                    <Grid item xs={6}>
                                        {showReferrer ? (
                                            <>
                                                <Typography gutterBottom className={classes.mt} variant="h6">
                                                    推荐人
                                                </Typography>
                                                <TextField
                                                    className={classes.inputItem}
                                                    size="small"
                                                    variant="outlined"
                                                    placeholder="请输入推荐人"
                                                    value={referrer}
                                                    onChange={(e) => setReferrer(e.target.value)}/>
                                            </>
                                        ) : (
                                            <Button onClick={() => setShowReferrer(true)}
                                                    color="primary">有推荐人?</Button>
                                        )}
                                    </Grid>
                                </Grid>
                            </Grid>
                        </Grid>
                    </>
                )}
                openDialog={openPurchaseDialog}
                setOpenDialog={setOpenPurchaseDialog}
                doDialog={() => {
                    return apis.purchase({
                        productId: operateItem.productId,
                        balanceMethod: balanceMethod,
                        referrer: referrer
                    }).then(res => {
                        if (res.status === 0) {
                            setRefresh(+new Date());
                            setOperateItem({});
                            setBalanceMethod('METHOD_HOUR');
                            setOpenPurchaseDialog(false);
                            return '操作成功';
                        }
                        throw new Error(res.message);
                    });
                }}
                okText="保存"
                okType="primary"/>
        </Card>
    );
};

DataTable.propTypes = {
    className: PropTypes.string,
    data: PropTypes.array.isRequired
};

export default withRouter(DataTable);
