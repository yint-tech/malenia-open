import React, {useEffect, useState} from "react";
import {Loading, OpeDialog} from "./index";
import {createUseStyles, useTheme} from "react-jss";
import {FormControlLabel, Radio, RadioGroup, TextField} from "@mui/material";
import {withRouter} from "react-router-dom";

const useStyles = createUseStyles({
    root: {},
    dialogInput: {
        width: "100%"
    },
    form: ({theme}) => ({
        paddingLeft: 70,
        paddingRight: 70,
        paddingBottom: 80,
        flexBasis: 700,
        [theme.breakpoints.down('sm')]: {
            paddingLeft: theme.spacing(2),
            paddingRight: theme.spacing(2)
        }
    }),
});

const AuthenticationDialog = (props) => {
    const {
        openAuthDialog, setOpenAuthDialog, callback, authTargetUser,
        title, authFunction
    } = props;

    const [opUser, setOpUser] = useState({})
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        let value = {...authTargetUser};
        if (!value.authenticationType) {
            value.authenticationType = "person"
        }
        setOpUser(value)
    }, [authTargetUser])

    const authType = (user) => {
        return user['authenticationType'] && user['authenticationType'].indexOf('enterprise') >= 0
            ? "enterprise" : "person";
    };

    const theme = useTheme();
    const classes = useStyles({theme});
    return (<OpeDialog
        title={title}
        opeContent={
            loading ? <Loading/> :
                (<form className={classes.form} novalidate autocomplete="off">
                        <RadioGroup aria-label="authentication" name="authentication"
                                    value={authType(opUser)}
                                    onChange={(e) => setOpUser({
                                        ...opUser,
                                        authenticationType: e.target.value
                                    })}
                                    row>
                            <FormControlLabel value="person" control={<Radio/>} label="个人认证"/>
                            <FormControlLabel value="enterprise" control={<Radio/>} label="企业认证"/>
                        </RadioGroup>

                        {authType(opUser) === "person" ? (
                            <>
                                <TextField
                                    label="姓名"
                                    key={"authenticationRealName"}
                                    value={opUser.authenticationRealName}
                                    onChange={(e) => setOpUser({
                                        ...opUser,
                                        authenticationRealName: e.target.value
                                    })}
                                />
                                <TextField
                                    label="身份证号码"
                                    key={"authenticationIdCard"}
                                    value={opUser.authenticationIdCard}
                                    onChange={(e) => setOpUser({
                                        ...opUser,
                                        authenticationIdCard: e.target.value
                                    })}
                                />
                            </>) : (
                            <>
                                <TextField
                                    label="企业名称"
                                    key={"authenticationCompanyName"}
                                    value={opUser.authenticationCompanyName}
                                    onChange={(e) => setOpUser({
                                        ...opUser,
                                        authenticationCompanyName: e.target.value
                                    })}
                                />
                                <TextField
                                    key={"authenticationLicenseNumber"}
                                    label="统一社会信用代码"
                                    value={opUser.authenticationLicenseNumber}
                                    onChange={(e) => setOpUser({
                                        ...opUser,
                                        authenticationLicenseNumber: e.target.value
                                    })}
                                />
                                <TextField
                                    key={"authenticationCorporateName"}
                                    label="法人姓名"
                                    value={opUser.authenticationCorporateName}
                                    onChange={(e) => setOpUser({
                                        ...opUser,
                                        authenticationCorporateName: e.target.value
                                    })}
                                />
                            </>
                        )}
                        <TextField
                            style={{marginTop: 10}}
                            className={classes.dialogInput}
                            label="认证备注"
                            size="small"
                            value={opUser.authenticationComment}
                            onChange={(e) => {
                                setOpUser({
                                    ...opUser,
                                    authenticationComment: e.target.value,
                                })
                            }}/>
                    </form>
                )}
        openDialog={openAuthDialog}
        setOpenDialog={setOpenAuthDialog}
        doDialog={() => {
            setLoading(true)
            return authFunction(opUser).then(res => {
                if (res.status === 0) {
                    callback && callback();
                    return true;
                }
                return false;
            }).finally(() => {
                setLoading(false)
            });
        }}
        okText="保存"
        okType="primary"/>);
}

export default withRouter(AuthenticationDialog);