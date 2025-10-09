import React, {useCallback, useContext, useEffect, useState} from 'react';
import {useHistory, useParams, withRouter} from 'react-router-dom';
import {AppContext} from "adapter";
import TurningParamsEditor from "./TurningParamsEditor";
import {createUseStyles, useTheme} from "react-jss";
import {
    Alert,
    AlertTitle, Button,
    Card, CardActions,
    CardContent,
    CardHeader,
    Divider,
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
});


const ProductEditor = () => {
        const history = useHistory();
        const params = useParams();
        const {api} = useContext(AppContext);

        const theme = useTheme();
        const classes = useStyles({theme});
        const [form, setForm] = useState(() => {
            return {
                productId: '',
                productName: '',
                description: '',
                mappingPortSpace: '',
                tuningParam: {"items": []},
                enabled: false,
                privateProduct: false,
                hourPrice: '',
                flowPrice: '',
                features: '',
            }
        });
        const [productIpResources, setProductIpResources] = useState(undefined);
        const [editTurningParam, setEditTuringParams] = useState({"items": []});
        const turningParamCallback = useCallback((newParams) => {
            setEditTuringParams(newParams)
        }, [])

        useEffect(() => {
            api.listAllProducts().then(res => {
                if (res.status === 0) {
                    let editProduct = res.data.find(item => item.productId === params.id);
                    if (editProduct) {
                        for (let i in editProduct) {
                            editProduct[i] = editProduct[i] || '';
                        }
                        setForm({...editProduct})
                    }
                }
            })

        }, [api, params.id]);


        useEffect(() => {
            if (!form || !form.productId) {
                return;
            }
            api.listProductIpSources({productId: form.productId}).then((res) => {
                if (res.status === 0) {
                    setProductIpResources(res.data);
                }
            });
        }, [form, api])


        const doSave = () => {
            api.editProduct({...form, tuningParam: editTurningParam}).then(res => {
                if (res.status === 0) {
                    history.go(-1);
                }
            })
        }

        return (
            <Card className={classes.root}>
                {productIpResources && productIpResources.length === 0 ? (
                    <Alert severity="warning">
                        <AlertTitle>友情提示</AlertTitle>
                        该产品尚未绑定弹性IP资源，请注意绑定弹性IP资源，否则无法使用。
                    </Alert>
                ) : (<></>)}
                <CardHeader title={!form.id ? '新增产品' : '编辑产品'}></CardHeader>
                <Divider/>
                <CardContent>
                    <Grid container spacing={6} wrap="wrap">
                        <Grid item xs={6}>
                            <Typography
                                gutterBottom
                                variant="h6"
                            >产品id</Typography>
                            <TextField
                                disabled={!!form.id}
                                className={classes.inputItem}
                                size="small"
                                variant="outlined"
                                placeholder="请填写产品id"
                                value={form.productId}
                                onChange={(e) => setForm({
                                    ...form,
                                    productId: e.target.value
                                })}/>
                        </Grid>
                        <Grid item xs={6}>
                            <Typography
                                gutterBottom
                                variant="h6"
                            >产品名称</Typography>
                            <TextField
                                className={classes.inputItem}
                                size="small"
                                variant="outlined"
                                placeholder="请填写产品名称"
                                value={form.productName}
                                onChange={(e) => setForm({
                                    ...form,
                                    productName: e.target.value
                                })}/>
                        </Grid>
                        <Grid item xs={4}>
                            <Typography
                                gutterBottom
                                variant="h6"
                            >产品描述</Typography>
                            <TextField
                                className={classes.inputItem}
                                size="small"
                                variant="outlined"
                                placeholder="请填写产品描述"
                                value={form.description}
                                onChange={(e) => setForm({
                                    ...form,
                                    description: e.target.value
                                })}/>
                        </Grid>

                        <Grid item xs={4}>
                            <Typography gutterBottom variant="h6">是否启用</Typography>
                            <Switch color="primary" checked={form.enabled} onChange={(e) => setForm(
                                {...form, enabled: e.target.checked})}/>
                        </Grid>
                        <Grid item xs={4}>
                            <Typography gutterBottom variant="h6">是否私有IP</Typography>
                            <Switch color="primary" checked={form.privateProduct} onChange={(e) => setForm(
                                {...form, privateProduct: e.target.checked})}/>
                        </Grid>

                        <Grid item xs={12}>
                            <Typography
                                gutterBottom
                                variant="h6"
                            >产品特征标签</Typography>
                            <TextField
                                className={classes.inputItem}
                                size="small"
                                variant="outlined"
                                placeholder="请输入产品特征标签，eg. "
                                value={form.features}
                                onChange={(e) => setForm({
                                    ...form,
                                    features: e.target.value
                                })}/>
                        </Grid>
                        <Grid item xs={6}>
                            <Typography
                                gutterBottom
                                variant="h6"
                            >使用价格，按小时付费</Typography>
                            <TextField
                                className={classes.inputItem}
                                size="small"
                                variant="outlined"
                                placeholder="请输入按小时付费价格，eg. 10.2"
                                value={form.hourPrice}
                                onChange={(e) => setForm({
                                    ...form,
                                    hourPrice: e.target.value
                                })}/>
                        </Grid>
                        <Grid item xs={6}>
                            <Typography
                                gutterBottom
                                variant="h6"
                            >使用价格，按流量付费(0.01G)</Typography>
                            <TextField
                                className={classes.inputItem}
                                size="small"
                                variant="outlined"
                                placeholder="请输入按流量付费价格，eg. 10.2"
                                value={form.flowPrice}
                                onChange={(e) => setForm({
                                    ...form,
                                    flowPrice: e.target.value
                                })}/>
                        </Grid>
                        <Grid item xs={12}>
                            <Typography
                                gutterBottom
                                variant="h6"
                            >端口范围</Typography>
                            <TextField
                                className={classes.inputItem}
                                size="small"
                                variant="outlined"
                                placeholder="请输入端口范围，eg. 8000-9000,9001,9002"
                                value={form.mappingPortSpace}
                                onChange={(e) => setForm({
                                    ...form,
                                    mappingPortSpace: e.target.value
                                })}/>
                        </Grid>
                        <Grid item xs={12}>
                            <TurningParamsEditor
                                turningParams={form.tuningParam}
                                onChange={turningParamCallback}
                            />
                        </Grid>
                    </Grid>
                </CardContent>
                <CardActions className={classes.actions}>
                    <Button className={classes.btns} variant="contained" color="secondary"
                            onClick={() => history.go(-1)}>退出</Button>
                    <Button className={classes.btns} variant="contained" color="primary" onClick={doSave}>保存</Button>
                </CardActions>
            </Card>
        );
    }
;

export default withRouter(ProductEditor);
