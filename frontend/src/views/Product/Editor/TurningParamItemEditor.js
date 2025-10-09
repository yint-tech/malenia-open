import {OpeDialog} from "components";
import React, {useEffect, useState} from "react";
import {createUseStyles, useTheme} from "react-jss";
import {Button, Grid, Switch, TextField, Typography} from "@mui/material";
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

const TurningParamItemEditor = (props) => {
    const {
        config,
        showEditDialog,
        setShowEditDialog,
        onSubmit
    } = props;
    const {
        addModel,
        editItem,
    } = config;

    const theme = useTheme();
    const classes = useStyles({theme});

    const [model, setModel] = useState({});
    const [editEnumItem, setEditEnumItem] = useState('');

    useEffect(() => {
        setModel(editItem)
    }, [editItem])

    return (<OpeDialog
        title={addModel ? "添加参数" : "编辑参数"}
        openDialog={showEditDialog}
        setOpenDialog={setShowEditDialog}
        doDialog={() => {
            onSubmit(model);
            return true;
        }}
        opeContent={(<div>
            <Grid container spacing={6} wrap="wrap">
                <Grid item xs={6}>
                    <Typography
                        gutterBottom
                        variant="h6"
                    >参数名词</Typography>
                    <TextField
                        className={classes.inputItem}
                        size="small"
                        variant="outlined"
                        placeholder="请填写参数名称"
                        value={model.param}
                        onChange={(e) => setModel({
                            ...model,
                            param: e.target.value
                        })}/>
                </Grid>
                <Grid item xs={6}>
                    <Typography
                        gutterBottom
                        variant="h6"
                    >参数描述</Typography>
                    <TextField
                        className={classes.inputItem}
                        size="small"
                        variant="outlined"
                        placeholder="参数描述"
                        value={model.description}
                        onChange={(e) => setModel({
                            ...model,
                            description: e.target.value
                        })}/>
                </Grid>
                <Grid item xs={6}>
                    <Typography
                        gutterBottom
                        variant="h6"
                    >是否必须</Typography>
                    <Switch
                        value={!model.nullable}
                        onChange={(e) => {
                            setModel({
                                ...model,
                                nullable: e.target.checked
                            })
                        }}
                    />
                </Grid>
                <Grid item xs={12}>
                    <Typography
                        gutterBottom
                        variant="h6"
                    >可选枚举</Typography>
                    {
                        (model.enums || []).map((item) =>
                            <Button
                                key={item}
                                size="small"
                                className={classes.tableButton}
                                onClick={() => {
                                    setModel({
                                        ...model,
                                        enums: model.enums.filter(it => it !== item)
                                    })
                                }}>
                                <Typography variant="subtitle2" style={{color: '#546e7a'}}>
                                    {item}
                                </Typography>
                            </Button>
                        )
                    }
                    <TextField
                        className={classes.inputItem}
                        size="small"
                        variant="outlined"
                        placeholder="枚举名称"
                        value={editEnumItem}
                        onChange={(e) => setEditEnumItem(e.target.value)}/>
                    <Button
                        size="small"
                        className={classes.tableButton}
                        onClick={() => {
                            if (editEnumItem && model.enums.indexOf(editEnumItem) < 0) {
                                setModel({
                                    ...model,
                                    enums: [...model.enums, editEnumItem]
                                })
                                setEditEnumItem("")
                            }
                        }}>
                        新增
                    </Button>
                </Grid>
            </Grid>
        </div>)}
    />)
}

export default withRouter(TurningParamItemEditor);