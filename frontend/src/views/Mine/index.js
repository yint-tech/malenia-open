import React, {useState} from 'react';
import {createUseStyles, useTheme} from "react-jss";
import UserDashboard from './UserDashBoard';
import WhiteIp from "./WhiteIp";
import UserMetric from "./Metric";
import Bill from "./Bill";
import Recharge from "./Recharge";
import {Card, Paper, Tab, Tabs} from "@mui/material";

const useStyles = createUseStyles({
    root: {
        padding: ({theme}) => theme.spacing(3),
        height: "100%"
    }, content: {
        marginTop: ({theme}) => theme.spacing(2)
    }
});

function TabPanel(props) {
    const {children, value, index} = props;
    return value === index ? children : null;
}


const Mine = () => {
    const theme = useTheme();
    const classes = useStyles({theme});

    const [value, setValue] = useState(0);
    const handleChange = (event, val) => {
        setValue(val);
    };

    return (
        <div className={classes.root}>
            <Paper className={classes.content}>
                <Tabs
                    value={value}
                    indicatorColor="primary"
                    textColor="primary"
                    onChange={handleChange}
                >
                    <Tab label="概览"/>
                    <Tab label="扣费账单"/>
                    <Tab label="充值记录"/>
                    <Tab label="白名单"/>
                    <Tab label="监控指标"/>
                </Tabs>
                <Card className={classes.content}>
                    <TabPanel value={value} index={0}>
                        <UserDashboard/>
                    </TabPanel>
                    <TabPanel value={value} index={1}>
                        <Bill/>
                    </TabPanel>
                    <TabPanel value={value} index={2}>
                        <Recharge/>
                    </TabPanel>
                    <TabPanel value={value} index={3}>
                        <WhiteIp/>
                    </TabPanel>
                    <TabPanel value={value} index={4}>
                        <UserMetric/>
                    </TabPanel>
                </Card>
            </Paper>
        </div>
    );
};

export default Mine;
