import React, {useContext, useEffect, useState} from 'react';
import {AppContext} from 'adapter';
import moment from 'moment';
import {Alert, AlertTitle, Button, CardContent, CardHeader, Divider, Grid, TextField} from "@mui/material";

const AuthUserConfig = () => {
    const {user, api, setUser} = useContext(AppContext);

    const [proxyName, setProxyName] = useState('');
    const [proxyPassword, setProxyPassword] = useState('');

    useEffect(() => {
        setProxyName(user.authAccount || '');
        setProxyPassword(user.authPwd || '');
    }, [user]);

    const saveProxyAccount = () => {
        api.setupAuthAccount({
            authAccount: proxyName,
            authPassword: proxyPassword
        }).then(res => {
            if (res.status === 0) {
                let temp = {...user, ...res.data};
                api.setStore(temp);
                setUser({
                    ...temp,
                    time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss')
                });
            }
        })
    }

    return (
        <>
            <CardHeader title="代理账号密码设置"/>
            {!user.authAccount && (
                <Alert severity="warning">
                    <AlertTitle>紧急！！！</AlertTitle>
                    未设置代理账号密码服务将容易泄漏账号密码，请尽量设置独立的代理账号密码
                </Alert>
            )}
            <Divider/>
            <CardContent>
                <Grid container spacing={2}>
                    <Grid item xs={4}>
                        <TextField
                            style={{width: "100%"}}
                            size="small"
                            label="代理账户"
                            variant="outlined"
                            value={proxyName}
                            onChange={(e) => setProxyName(e.target.value)}/>
                    </Grid>
                    <Grid item xs={4}>
                        <TextField
                            style={{width: "100%"}}
                            size="small"
                            label="代理密码"
                            variant="outlined"
                            value={proxyPassword}
                            onChange={(e) => setProxyPassword(e.target.value)}/>
                    </Grid>
                    <Grid item xs={2}>
                        <Button fullWidth variant="contained" color="primary" onClick={saveProxyAccount}>
                            应用
                        </Button>
                    </Grid>
                </Grid>
            </CardContent>
        </>
    );
};

export default AuthUserConfig;
