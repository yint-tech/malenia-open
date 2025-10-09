import React, {useState} from "react";
import {createUseStyles, useTheme} from "react-jss";

import {useHistory, useParams, withRouter} from "react-router-dom";

import Editor from "../Editor";

import IpResourceItems from "./IpResouceItems";
import Metric from "./Metric";
import ErrorRecords from "./ErrorRecords"
import {Paper, Tab, Tabs} from "@mui/material";

const useStyles = createUseStyles({
    root: {
        flexGrow: 1,
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    image: {
        marginTop: 50,
        display: 'inline-block',
        maxWidth: '100%',
        width: 560
    }
});

function TabPanel(props) {
    const {children, value, index} = props;
    return value === index ? children : null;
}

function IpSourceWorkSpace() {
    const theme = useTheme();
    const classes = useStyles({theme});
    const params = useParams();
    const history = useHistory();
    const [value, setValue] = useState(0);

    const handleChange = (event, val) => {
        setValue(val);
    };

    if (!params['ipSourceKey']) {
        history.push("/not-found");
    }

    return (<>
        {!!params['ipSourceKey'] ? (<div className={classes.root}>
                <div className={classes.content}>
                    <Paper className={classes.root}>
                        <Tabs
                            value={value}
                            indicatorColor="primary"
                            textColor="primary"
                            onChange={handleChange}
                        >
                            <Tab label="配置"/>
                            <Tab label="资源列表"/>
                            <Tab label="监控"/>
                            <Tab label="报错记录"/>
                        </Tabs>
                        <div className={classes.content}>
                            <TabPanel value={value} index={0}>
                                <Editor ipSourceKey={params.ipSourceKey}/>
                            </TabPanel>
                            <TabPanel value={value} index={1}>
                                <IpResourceItems ipSourceKey={params.ipSourceKey}/>
                            </TabPanel>
                            <TabPanel value={value} index={2}>
                                <Metric ipSourceKey={params.ipSourceKey}/>
                            </TabPanel>
                            <TabPanel value={value} index={3}>
                                <ErrorRecords ipSourceKey={params.ipSourceKey}/>
                            </TabPanel>
                        </div>
                    </Paper>
                </div>
            </div>) :
            <img
                alt="Under development"
                className={classes.image}
                src="/images/undraw_page_not_found_su7k.svg"
            />
        }
    </>);
}

export default withRouter(IpSourceWorkSpace);