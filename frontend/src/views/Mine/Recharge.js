import React, {useContext, useState} from 'react';
import {AppContext} from 'adapter';
import moment from 'moment';
import SimpleTable from "components/SimpleTable";
import {OpeDialog} from "components";
import {createUseStyles, useTheme} from "react-jss";
import {
    Button,
    FormControl,
    FormControlLabel,
    FormLabel,
    Radio,
    RadioGroup,
    TextField,
    Typography
} from "@mui/material";

const useStyles = createUseStyles({
    root: {
        flexGrow: 1,
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    btn: {
        marginRight: ({theme}) => theme.spacing(1),
    },
    label: {
        marginBottom: ({theme}) => theme.spacing(2)
    }
});

const OnlineChargeDialog = (prop) => {
    const {openOnlineCharge, setOpenOnlineCharge} = prop;
    const theme = useTheme();
    const classes = useStyles({theme});
    const {api, systemInfo} = useContext(AppContext);
    const {wechatQrCode, setWechatQrCode} = useState('')

    const [value, setValue] = useState({
        'template': '1000',
        'custom': '1',
        'value': 1000
    });
    const [payType, setPayTypeValue] = useState(systemInfo.buildInfo.supportAlipay ? 'Alipay' : 'Wechat');

    const submit = () => {
        if (parseInt(value.value) < 0) {
            api.errorToast("充值金额错误");
            return false;
        }
        if (payType === 'Alipay') {
            api.prepare_alipay({
                    price: value.value,
                    "returnURL": window.location.origin
                }
            ).then(res => {
                if (res.status === 0) {
                    const data = res.data.body
                    const newTab = window.open('', '_blank');
                    newTab.document.open();
                    newTab.document.write('<!DOCTYPE html><html><head><meta charset="UTF-8"><title>支付宝二维码</title></head><body>' + data + '</body></html>');
                    newTab.document.close();
                }
            })
        } else {
            api.prepare_wechat({
                    price: value.value,
                    "returnURL": window.location.origin
                }
            ).then(res => {
                if (res.status === 0) {
                    setWechatQrCode(res.data)
                }
            })
        }

    }

    return (<OpeDialog
        title={"在线充值"}
        openDialog={openOnlineCharge}
        setOpenDialog={setOpenOnlineCharge}
        opeContent={
            (<>
                {!!wechatQrCode ? <img alt={"微信扫码支付"} src={wechatQrCode}/> :
                    <FormControl component="fieldset">
                        <FormLabel component="legend" className={classes.label}>
                            充值金额
                            <RadioGroup
                                row
                                aria-label="pay"
                                name="pay"
                                value={value.template}
                                onChange={(event) => {
                                    setValue({
                                        template: event.target.value,
                                        custom: '1',
                                        value: event.target.value
                                    })
                                }}>
                                <FormControlLabel value="5" control={<Radio/>} label="5"/>
                                <FormControlLabel value="10" control={<Radio/>} label="10"/>
                                <FormControlLabel value="100" control={<Radio/>} label="100"/>
                                <FormControlLabel value="1000" control={<Radio/>} label="1000"/>
                                <FormControlLabel value="5000" control={<Radio/>} label="5000"/>
                                <FormControlLabel value="10000" control={<Radio/>} label="10000"/>
                                <TextField
                                    label="其他金额"
                                    InputProps={{
                                        inputProps: {min: 1}
                                    }}
                                    type="number"
                                    value={value.custom}
                                    onChange={(event) => {
                                        let number = parseInt(event.target.value);
                                        if (number < 1) {
                                            number = 1;
                                        }
                                        setValue({
                                            template: '1000',
                                            custom: number,
                                            value: number
                                        })
                                    }}
                                ></TextField>
                            </RadioGroup>
                        </FormLabel>
                        <FormLabel component="legend" className={classes.label}>支付方式
                            <RadioGroup aria-label="pay" name="pay" value={payType}
                                        onChange={(event) => {
                                            setPayTypeValue(event.target.value)
                                        }} row>
                                <FormControlLabel value="Alipay" control={<Radio/>} label="支付宝"
                                                  disabled={!systemInfo.buildInfo.supportAlipay}/>
                                <FormControlLabel value="Wechat" control={<Radio/>} label="微信"
                                                  disabled={!systemInfo.buildInfo.supportWechatPay}/>
                            </RadioGroup>
                        </FormLabel>
                        <FormLabel component="legend" className={classes.label}> 支付金额</FormLabel>
                        <Typography className={classes.coloredText}>
                            {value.value ? value.value + "元" : ""}
                        </Typography>
                        <Button variant="contained" color="primary" className={classes.btn} onClick={submit}>
                            立即充值
                        </Button>
                    </FormControl>}
            </>)
        }
    />)
}

const Recharge = () => {
    const {api, user, systemInfo} = useContext(AppContext);
    const [openOnlineCharge, setOpenOnlineCharge] = useState(false);
    const hasAuthentication = !!(user['authenticationThirdPartyId']);
    return (
        <SimpleTable
            actionEl={
                (systemInfo.buildInfo.supportAlipay || systemInfo.buildInfo.supportWechatPay) ?
                    (<>
                            <Button
                                style={{marginRight: 10}}
                                color="primary"
                                size="medium"
                                variant="contained"
                                onClick={() => {
                                    if (!hasAuthentication) {
                                        api.errorToast("请先完成实名认证");
                                        return;
                                    }
                                    setOpenOnlineCharge(true)
                                }}>在线充值</Button>
                            <OnlineChargeDialog
                                openOnlineCharge={openOnlineCharge}
                                setOpenOnlineCharge={setOpenOnlineCharge}
                            />
                        </>
                    ) : <></>}
            loadDataFun={
                api.rechargeRecord
            }
            columns={[
                {
                    label: "操作人",
                    key: "operator"
                }, {
                    label: "到账金额",
                    key: "rechargeAmount"
                }, {
                    label: "充值后余额",
                    key: "remainBalance"
                }, {
                    label: "充值总额",
                    key: "totalActualPayAmount"
                }, {
                    label: "充值时间",
                    render: (item) => moment(new Date(item.createTime)).format("YYYY-MM-DD HH:mm:ss")
                },
                {
                    label: "备注",
                    key: "comment"
                }
            ]}/>
    );
};

export default Recharge;
