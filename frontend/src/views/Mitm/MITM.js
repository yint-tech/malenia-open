import React, {useState} from "react";
import FileAssets from "./FileAssets";
import MitmScript from "./MitmScript";
import MitmLogs from "./MitmLogs";
import {createUseStyles, useTheme} from "react-jss";
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

function TabPanel(props) {
    const {children, value, index} = props;
    return value === index ? children : null;
}

function MITM() {
    const theme = useTheme();
    const classes = useStyles({theme});

    const [value, setValue] = useState(0);

    const handleChange = (event, val) => {
        setValue(val);
    };

    return (
        <div className={classes.root}>
            <div className={classes.content}>
                <Paper className={classes.root}>
                    <Tabs
                        value={value}
                        indicatorColor="primary"
                        textColor="primary"
                        onChange={handleChange}
                    >
                        <Tab label="脚本管理"/>
                        <Tab label="资产文件"/>
                        <Tab label="mitm日志"/>
                        <Tab label="抓包控制台"/>
                    </Tabs>
                    <div className={classes.content}>
                        <TabPanel value={value} index={0}>
                            <MitmScript/>
                        </TabPanel>
                        <TabPanel value={value} index={1}>
                            <FileAssets/>
                        </TabPanel>
                        <TabPanel value={value} index={2}>
                            <MitmLogs/>
                        </TabPanel>
                        <TabPanel value={value} index={3}>
                            <p>TODO</p>
                        </TabPanel>
                    </div>
                </Paper>
            </div>
        </div>
    );
}

export default withRouter(MITM);
