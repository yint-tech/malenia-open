import React, {useEffect, useState} from 'react';

import {Table} from "components"
import {createUseStyles, useTheme} from "react-jss";
import TurningParamItemEditor from "./TurningParamItemEditor";
import {Button, Card, CardActions, CardContent, CardHeader} from "@mui/material";
import {Delete, Details} from "@mui/icons-material";
import {withRouter} from "react-router-dom";


const useStyles = createUseStyles({
    root: {
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    tableButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
    row: {
        height: '42px',
        display: 'flex',
        alignItems: 'center',
        marginTop: ({theme}) => theme.spacing(1)
    },

});
const demo = [
    {
        "param": "country",
        "nullable": true,
        "description": "国家",
        "enums": [
            "us", "uk", "cn", "*"
        ]
    },
    {
        "param": "zone",
        "nullable": true,
        "description": "区域",
        "enums": [
            "zone1", "zone2"
        ]
    },
    {
        "param": "session",
        "nullable": true,
        "description": "会话标记，可以是任意随机字符串",
        "enums": []
    }
];

const itemTemplate = () => {
    return {
        "param": "",
        "nullable": true,
        "description": "",
        "enums": []
    }
}

const TurningParamsEditor = (props) => {
    const {turningParams, onChange} = props;
    const theme = useTheme();
    const classes = useStyles({theme});
    const [paramList, setParamList] = useState([]);

    const [showEditDialog, setShowEditDialog] = useState(false);
    const [editItemConfig, setEditItemConfig] = useState({
        addModel: false,
        editItem: itemTemplate()
    })


    useEffect(() => {
        setParamList(turningParams['items']);
    }, [turningParams])

    useEffect(() => {
        onChange({
            "items": paramList
        })
    }, [onChange, paramList])


    return (
        <Card>
            <CardHeader
                title={"隧道路由参数"}
                action={(
                    <>
                        <Button
                            startIcon={<Details style={{fontSize: 16}}/>}
                            size="small"
                            color="primary"
                            onClick={() => {
                                setEditItemConfig({
                                    addModel: true,
                                    editItem: itemTemplate()
                                });
                                setShowEditDialog(true);
                            }}
                            className={classes.tableButton}
                            variant="contained">新增参数</Button>
                        <Button
                            startIcon={<Details style={{fontSize: 16}}/>}
                            size="small"
                            color="primary"
                            onClick={() => {
                                setParamList(demo)
                            }}
                            className={classes.tableButton}
                            variant="contained">加载样例</Button></>
                )}
            />
            <CardContent>
                <Table
                    size="small"
                    data={paramList}
                    columns={
                        [{
                            label: '参数名',
                            key: 'param'
                        }, {
                            label: '必须',
                            render: (item) => {
                                return item.nullable ? "否" : "是"
                            }
                        }, {
                            label: '参数解释',
                            key: 'description'
                        }, {
                            label: '参数枚举',
                            render: (item) => {
                                return (<div>  {item.enums.map(item =>
                                    (<Button
                                        size={"small"}
                                        className={classes.tableButton}
                                        variant="contained">
                                        {item}
                                    </Button>)
                                )}
                                </div>)
                            }
                        }, {
                            label: '操作',
                            render: (item) => (
                                <>
                                    <Button
                                        startIcon={<Delete style={{fontSize: 16}}/>}
                                        size="small"
                                        color="primary"
                                        onClick={() => {
                                            setParamList(paramList.filter((i) => i !== item))
                                        }}
                                        className={classes.tableButton}
                                        variant="contained">删除</Button>
                                    <Button
                                        startIcon={<Details style={{fontSize: 16}}/>}
                                        size="small"
                                        color="primary"
                                        onClick={() => {
                                            setEditItemConfig({
                                                addModel: false,
                                                editItem: item
                                            });
                                            setShowEditDialog(true);
                                        }}
                                        className={classes.tableButton}
                                        variant="contained">编辑</Button>
                                </>
                            )
                        },
                        ]
                    }
                />
            </CardContent>
            <CardActions>
                <TurningParamItemEditor
                    config={editItemConfig}
                    showEditDialog={showEditDialog}
                    setShowEditDialog={setShowEditDialog}
                    onSubmit={(config) => {
                        let index = paramList.findIndex((item) => item.param === config.param);
                        if (index < 0) {
                            setParamList([...paramList, config])
                        } else {
                            let newParams = [...paramList];
                            newParams[index] = config;
                            setParamList(newParams);
                        }
                    }}

                />
            </CardActions>
        </Card>

    )
};
export default withRouter(TurningParamsEditor);

