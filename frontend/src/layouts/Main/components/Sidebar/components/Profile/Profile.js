import React, {useCallback, useContext, useState} from 'react';
import {AppContext} from 'adapter';
import PropTypes from 'prop-types';
import {Alert, Avatar, Button, Typography} from "@mui/material";
import {Link as RouterLink} from 'react-router-dom';
import {createUseStyles, useTheme} from "react-jss";
import AuthenticationDialog from "components/AuthenticationDialog";
import moment from "moment";

const useStyles = createUseStyles({
    avatar: {
        width: 60,
        height: 60
    },
    user: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: ({theme}) => theme.spacing(1),
    },
    line: {
        height: ({theme}) => theme.spacing(2),
        marginLeft: ({theme}) => theme.spacing(1),
        marginRight: ({theme}) => theme.spacing(1)
    },
    name: {
        marginLeft: ({theme}) => theme.spacing(1),
    },
    setting: {
        fontSize: 14,
        display: 'flex',
        alignItems: 'center',
    }
});

const Profile = () => {
    const {user, setUser, notice, systemInfo, api} = useContext(AppContext);
    const theme = useTheme();
    const classes = useStyles({theme});

    const [openAuthDialog, setOpenAuthDialog] = useState(false);
    const hasAuthentication = !!(user['authenticationThirdPartyId']);
    const reloadProfile = useCallback(() => {
        api.getUser().then((res) => {
            if (res.status === 0) {
                api.setStore(res.data);
                setUser({
                    ...res.data,
                    time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss')
                });
            }
        })
    }, [api, setUser])

    return (
        <div>
            <div className={classes.user}>
                <Avatar
                    className={classes.purple}
                    component={RouterLink}
                    to="/"
                >
                    {user.userName ? user.userName[0] : ''}
                </Avatar>
                <Typography
                    className={classes.name}
                    variant="h3"
                >
                    {user.userName}
                </Typography>
            </div>
            <div className={classes.user}>
                <Typography variant="h5">余额: {user.balance}</Typography>
            </div>
            {
                systemInfo.buildInfo.supportCertification ? (
                    hasAuthentication ? (
                        <div className={classes.user}>
                            <Typography
                                color={"textSecondary"}
                                variant="h6">已实名: {(user.authenticationType || '').indexOf('enterprise') >= 0 ? '企业' : '个人'}</Typography>
                        </div>) : <>
                        <Alert
                            className={classes.user}
                            severity="warning"
                            action={
                                <Button color="inherit"
                                        size="small"
                                        onClick={() => {
                                            setOpenAuthDialog(true)
                                        }}
                                >去实名</Button>
                            }
                        >完成实名认证</Alert>
                        <AuthenticationDialog
                            openAuthDialog={openAuthDialog}
                            setOpenAuthDialog={setOpenAuthDialog}
                            authTargetUser={user}
                            callback={reloadProfile}
                            title={"实名认证"}
                            authFunction={api.certification}
                        /></>
                ) : null
            }
            {notice ? (
                <Alert severity="info">{notice}</Alert>
            ) : null}
        </div>
    );
};

Profile.propTypes = {
    className: PropTypes.string
};

export default Profile;
