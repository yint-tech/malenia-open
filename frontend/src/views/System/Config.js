import React, {useContext, useEffect, useState} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Button,
    Card,
    Divider,
    Grid,
    Switch,
    TextField,
    Typography
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {Loading} from "components";
import {AppContext} from "adapter";
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {
        margin: ({theme}) => theme.spacing(4)
    },
    heading: {
        fontSize: ({theme}) => theme.typography.pxToRem(15),
        flexBasis: "33.33%",
        flexShrink: 0
    },
    secondaryHeading: {
        fontSize: ({theme}) => theme.typography.pxToRem(15),
        color: ({theme}) => theme.palette.text.secondary,
        maxWidth: "300px",
        overflow: "hidden",
        whiteSpace: "nowrap",
        textOverflow: "ellipsis"
    },
    desc: {
        fontSize: ({theme}) => theme.typography.pxToRem(15),
        color: ({theme}) => theme.palette.text.secondary
    },
    input: {
        display: "flex",
        alignItems: "center"
    },
    inputItem: {
        width: "100%"
    },
    inputBtn: {
        marginLeft: ({theme}) => theme.spacing(2)
    },
    gutterTop: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    divider: {
        marginTop: ({theme}) => theme.spacing(1),
        marginBottom: ({theme}) => theme.spacing(2)
    },
    actions: {
        justifyContent: "center"
    },
    noMaxWidth: {
        maxWidth: "none"
    }
});

function SingleInputItem({
                             placeholder = "",
                             initValue = "",
                             initKey = "",
                             type = "String",
                             reload = () => {
                             }
                         }) {
    const theme = useTheme();
    const classes = useStyles({theme});
    const {api} = useContext(AppContext);
    const [value, setValue] = useState("");
    useEffect(() => {
        setValue(initValue);
    }, [initValue]);

    const doSave = () => {
        api.setConfig({key: initKey, value}).then(res => {
            if (res.status === 0) {
                api.successToast("修改成功");
            }
            reload();
        })
    };

    let multiLine = type === "multiLine";

    return (
        <Grid item xs={12} className={classes.input}>
            {
                type === "String" || multiLine ? (
                    <TextField
                        className={classes.inputItem}
                        multiline={multiLine}
                        minRows={multiLine ? 4 : undefined}
                        size="small"
                        variant="outlined"
                        placeholder={placeholder}
                        value={value}
                        onChange={(e) => setValue(e.target.value)}/>
                ) : null
            }
            {
                type === "Integer" ? (
                    <TextField
                        className={classes.inputItem}
                        type={"number"}
                        size="small"
                        variant="outlined"
                        placeholder={placeholder}
                        value={value}
                        onChange={(e) => setValue(e.target.value)}/>
                ) : null
            }
            {
                type === "Boolean" ? (
                    <Switch
                        checked={value || false}
                        onChange={(e) => setValue(e.target.checked)}
                        inputProps={{"aria-label": "secondary checkbox"}}
                    />
                ) : null
            }
            <Button className={classes.inputBtn} variant="contained" color="primary" onClick={doSave}>保存</Button>
        </Grid>
    );
}

const Form = () => {
    const theme = useTheme();
    const classes = useStyles({theme});
    const [configs, setConfigs] = useState([]);

    const [refresh, setRefresh] = useState(+new Date());
    const {api} = useContext(AppContext);
    useEffect(() => {
        api.settingTemplate().then(res => {
            if (res.status === 0) {
                setConfigs(res.data.normal);
            }
        })
    }, [api, refresh]);

    return (
        <Card className={classes.root}>
            {configs.length > 0 ? <>
                    {configs.map((item, index) => (
                        <Accordion key={"panel" + index}>
                            <AccordionSummary expandIcon={<ExpandMoreIcon/>}>
                                <Typography className={classes.heading}>{item.desc}</Typography>
                                <Typography className={classes.secondaryHeading}>{item.key}</Typography>
                            </AccordionSummary>
                            <AccordionDetails>
                                <div style={{width: "100%"}}>
                                    <Typography className={classes.desc}>填写说明：{item.detailDesc}</Typography>
                                    <Divider className={classes.divider}/>
                                    <Grid container spacing={6} wrap="wrap">
                                        <SingleInputItem
                                            type={item.type}
                                            placeholder={item.desc}
                                            initKey={item.key}
                                            initValue={item.value}
                                            reload={() => setRefresh(+new Date())}/>
                                    </Grid>
                                </div>
                            </AccordionDetails>
                        </Accordion>
                    ))}
                </> :
                <Loading/>
            }
        </Card>
    );
};

export default Form;
