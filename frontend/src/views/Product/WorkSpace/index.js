import React, {useContext, useEffect, useState} from "react";
import {createUseStyles, useTheme} from "react-jss";
import {useHistory, useParams, withRouter} from "react-router-dom";

import Editor from "../Editor";
import IpSourceRatio from "./IpSourceRatio"
import {Loading} from "components";
import {AppContext} from "adapter";
import Metric from "./Metric";
import {Card, CardContent, Grid, Paper, Tab, Tabs} from "@mui/material";

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


const ProxyServerListPanel = ({productId}) => {
    const {api} = useContext(AppContext);

    const [portList, setProxyList] = useState([]);
    useEffect(() => {
        api.productPorts({productId: productId}).then(res => {
            if (res.status === 0) {
                setProxyList(res.data)
            }
        })
    }, [api, productId]);

    return (<Card>
        <CardContent>
            <Grid spacing={1} container wrap="wrap">
                {
                    portList.map((port) => {
                        return (<Grid key={port} item md={2} xs={3}>
                            <p> {window.location.hostname}:{port}</p>
                        </Grid>)
                    })
                }
            </Grid>
        </CardContent>
    </Card>)
}

function ProductWorkSpace() {
    const theme = useTheme();
    const classes = useStyles({theme});
    const params = useParams();
    const history = useHistory();
    const [value, setValue] = useState(0);

    const handleChange = (event, val) => {
        setValue(val);
    };

    if (!params['id']) {
        history.push("/not-found");
    }

    return (<>
        {!!params['id'] ? (<div className={classes.root}>
                <div className={classes.content}>
                    <Paper className={classes.root}>
                        <Tabs
                            value={value}
                            indicatorColor="primary"
                            textColor="primary"
                            onChange={handleChange}
                        >
                            <Tab label="配置"/>
                            <Tab label="弹性IP源"/>
                            <Tab label="代理端口"/>
                            <Tab label="监控"/>
                        </Tabs>
                        <div className={classes.content}>
                            <TabPanel value={value} index={0}>
                                <Editor id={params.id}/>
                            </TabPanel>
                            <TabPanel value={value} index={1}>
                                <IpSourceRatio id={params.id}/>
                            </TabPanel>
                            <TabPanel value={value} index={2}>
                                <ProxyServerListPanel productId={params.id}/>
                            </TabPanel>
                            <TabPanel value={value} index={3}>
                                <Metric productId={params.id}/>
                            </TabPanel>
                        </div>
                    </Paper>
                </div>
            </div>) :
            <Loading/>
        }
    </>);
}
export default withRouter(ProductWorkSpace);

