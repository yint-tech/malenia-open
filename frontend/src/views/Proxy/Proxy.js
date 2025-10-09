import React, {useEffect, useState} from "react";
import {createUseStyles, useTheme} from "react-jss";

import Product from "./Product";
import OrderList from "./OrderList";
import configs from "../../config";
import {Paper, Tab, Tabs} from "@mui/material";
import {withRouter} from "react-router-dom";

const useStyles = createUseStyles({
    root: {
        flexGrow: 1,
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    }
});

const proxyTabKey = configs.app + "-proxy-tab";

function TabPanel(props) {
    const {children, value, index} = props;
    return value === index ? children : null;
}

function Proxy() {
    const theme = useTheme();
    const classes = useStyles({theme});

    let initValue = Number(localStorage.getItem(proxyTabKey)) || 0;
    const [value, setValue] = useState(initValue);
    useEffect(() => {
        localStorage.setItem(proxyTabKey, value + "");
    }, [value])


    const handleChange = (event, val) => {
        setValue(val);
    };

    return (
        <div className={classes.root}>
            <div className={classes.content}>
                <Paper>
                    <Tabs
                        value={value}
                        indicatorColor="primary"
                        textColor="primary"
                        onChange={handleChange}
                    >
                        <Tab label="产品列表"/>
                        <Tab label="订单"/>
                    </Tabs>
                    <div className={classes.content}>
                        <TabPanel value={value} index={0}>
                            <Product/>
                        </TabPanel>
                        <TabPanel value={value} index={1}>
                            <OrderList/>
                        </TabPanel>
                    </div>
                </Paper>
            </div>
        </div>
    );
}

export default withRouter(Proxy);
