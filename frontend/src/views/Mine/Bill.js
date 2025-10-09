import React, {useContext, useEffect, useState} from 'react';
import {AppContext} from 'adapter';
import moment from 'moment';
import SimpleTable from "components/SimpleTable";
import apis from "apis";
import config from "config";
import {Button, MenuItem, Select} from "@mui/material";

const Bill = () => {
    const {api, user} = useContext(AppContext);
    const [showOrder, setShowOrder] = useState("all");
    const [orders, setOrders] = useState([{productId: "all", productName: "ALL"}]);

    useEffect(() => {
        api.listOrder().then(res => {
            if (res.status === 0) {
                setOrders([{productId: "all", productName: "ALL"}, ...res.data]);
            }
        })
    }, [api])

    return (
        <SimpleTable
            actionEl={<>
                <Button
                    style={{marginRight: 10}}
                    color="primary"
                    size="medium"
                    variant="contained"
                    onClick={() => {
                        let startDate = moment(new Date()).subtract(90, "days").format("yyyy-MM-DD HH:mm:ss");
                        let endDate = moment(new Date()).format("yyyy-MM-DD HH:mm:ss");
                        let url = `${apis.urls['exportBill']}?${config.login_token_key}=${user.loginToken}&startTime=${startDate}&endTime=${endDate}`;
                        window.open(url);
                    }}>导出 EXCEL</Button>
                <Select
                    style={{width: "200px", height: "40px", overflow: "hidden"}}
                    variant="outlined"
                    value={showOrder}
                    onChange={(e) => {
                        setShowOrder(e.target.value);
                    }}
                >
                    {orders.map(item => (
                        <MenuItem key={item.productId} value={item.productId}>
                            {item.productName}
                        </MenuItem>
                    ))}
                </Select>
            </>}
            loadDataFun={
                api.listBill
            }
            columns={
                [
                    {
                        label: "产品名",
                        key: "productName"
                    }, {
                    label: "价格",
                    key: "consumeAmount"
                }
                    , {
                    label: "当前余额",
                    key: "userBalance"
                }, {
                    label: "计费方式",
                    render: (item) => {
                        return {
                            "METHOD_HOUR": "按小时",
                            "METHOD_FLOW": "按流量(0.01G)"
                        }[item.balanceMethod];
                    }
                }, {
                    label: "时间",
                    key: "billTime"
                }
                ]
            }/>
    );
};

export default Bill;
