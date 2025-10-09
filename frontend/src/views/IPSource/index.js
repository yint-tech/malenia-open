import React, {useContext} from 'react';
import {createUseStyles, useTheme} from "react-jss";

import {SimpleTable} from 'components';
import {AppContext} from 'adapter';
import {useHistory, withRouter} from "react-router-dom";
import {AddPhotoAlternate, DeviceHub} from "@mui/icons-material";
import {Button, Card} from "@mui/material";

const useStyles = createUseStyles({
    root: {
        flexGrow: 1,
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    tagButton: {
        border: '1px dashed #f0f0f0',
        marginRight: ({theme}) => theme.spacing(1)
    }
});


const IPSourceList = () => {
    const theme = useTheme();
    const classes = useStyles({theme});
    const history = useHistory();
    const {api} = useContext(AppContext);

    return (
        <div className={classes.root}>
            <Card className={classes.content}>
                <SimpleTable
                    actionEl={(
                        <Button
                            startIcon={<AddPhotoAlternate/>}
                            color="primary"
                            variant="contained"
                            onClick={() => history.push('/createIpResource')}
                        >
                            创建IP源
                        </Button>
                    )}
                    loadDataFun={api.getAllIpSources}
                    columns={[
                        {
                            label: '资源名称',
                            key: 'sourceKey'
                        }, {
                            label: '描述',
                            key: 'description'
                        },
                        {
                            label: '资源池大小',
                            key: 'poolSize'
                        },
                        {
                            label: '资源解析器',
                            render: (item) => {
                                return item.loadResourceHandler.length > 64 ? item.loadResourceHandler.slice(0, 64) + "..." : item.loadResourceHandler
                            }
                        }, {
                            label: '操作',
                            render: (item) => (
                                <Button
                                    startIcon={<DeviceHub style={{fontSize: 16}}/>}
                                    size="small"
                                    color="primary"
                                    className={classes.tableButton}
                                    onClick={() => history.push('/IpResourceWorkSpace/' + item.sourceKey)}
                                    variant="contained">详情</Button>
                            )
                        }
                    ]}
                />
            </Card>
        </div>
    )
};

export default withRouter(IPSourceList);