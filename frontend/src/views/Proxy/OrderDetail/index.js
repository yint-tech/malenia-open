import React, {useState} from "react";
import {createUseStyles, useTheme} from "react-jss";
import {useHistory, useParams, withRouter} from "react-router-dom";
import {Loading} from "components";
import Metric from "./Metric";
import Editor from "./Editor";
import {Paper, Tab, Tabs} from "@mui/material";

const useStyles = createUseStyles({
    root: {
        flexGrow: 1,
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    }, image: {
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
                            <Tab label="监控"/>
                        </Tabs>
                        <div className={classes.content}>
                            <TabPanel value={value} index={0}>
                                <Editor productId={params.id}/>
                            </TabPanel>
                            <TabPanel value={value} index={1}>
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
