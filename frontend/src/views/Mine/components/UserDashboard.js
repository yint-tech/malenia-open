import React, {useContext} from "react";
import {
    Alert,
    AlertTitle,
    Button,
    Card,
    CardContent,
    CardHeader,
    Divider,
    Grid,
    IconButton,
    Popover,
    Typography
} from "@mui/material";
import {AppContext} from "adapter";
import {CopyToClipboard} from "react-copy-to-clipboard";
import FileCopyIcon from "@mui/icons-material/FileCopy";
import CachedIcon from "@mui/icons-material/Cached";
import moment from "moment";
import configs from 'config'
import clsx from "clsx";
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {
        height: "100%"
    },
    content: {
        alignItems: "center",
        display: "flex"
    },
    title: {
        fontWeight: 700
    },
    avatar: {
        backgroundColor: ({theme}) => theme.palette.success.main,
        height: 56,
        width: 56
    },
    icon: {
        height: 32,
        width: 32
    },
    mt: {
        marginTop: ({theme}) => theme.spacing(4)
    },
    mr: {
        marginRight: ({theme}) => theme.spacing(6)
    },
    pd: {
        width: ({theme}) => theme.spacing(18),
        paddingLeft: ({theme}) => theme.spacing(1),
        textAlign: "center"
    },
    url: {
        display: "flex",
        alignItems: "center",
        fontSize: 16,
        lineHeight: "1.2em",
        wordBreak: "break-all",
        cursor: "pointer"
    },
    padding: {
        padding: ({theme}) => theme.spacing(2)
    },
    formControl: {
        width: ({theme}) => theme.spacing(20),
        margin: ({theme}) => theme.spacing(2)
    },
    pop: {
        padding: ({theme}) => theme.spacing(2)
    },
    popBtns: {
        marginTop: ({theme}) => theme.spacing(2),
        textAlign: "center"
    }
});

const UserDashboard = props => {
    const {className, ...rest} = props;
    const {user, setUser} = useContext(AppContext);
    const {api} = useContext(AppContext);
    const apiUrl = user.apiToken;

    const theme = useTheme();
    const classes = useStyles({theme});


    const [anchorEl, setAnchorEl] = React.useState(null);
    const handleClick = (event) => {
        setAnchorEl(event.currentTarget);
    };
    const handleClose = () => {
        setAnchorEl(null);
    };

    const doRefreshApiToken = () => {
        api.regenerateAPIToken().then(res => {
            if (res.status === 0) {
                let user = api.getStore();
                user.apiToken = res.data.apiToken;
                api.setStore(user);
                setUser({
                    ...user,
                    time: moment(new Date()).format("YYYY-MM-DD HH:mm:ss")
                });
            }
        });
    };

    return (
        <Card
            {...rest}
            className={clsx(classes.root, className)}
        >
            <CardHeader title="API TOKEN"/>
            <Divider/>
            <CardContent>
                <Grid
                    container
                    spacing={2}
                >
                    <Grid item xs={12}>
                        <Typography
                            className={classes.url}
                            color="textSecondary"
                            variant="caption"
                        >
                            {apiUrl}
                            <CopyToClipboard text={apiUrl}
                                             onCopy={() => api.successToast("复制成功")}>
                                <IconButton style={{marginLeft: 15}} color="primary" aria-label="upload picture"
                                            component="span">
                                    <FileCopyIcon/>
                                </IconButton>
                            </CopyToClipboard>
                            <IconButton
                                onClick={handleClick}
                                style={{marginLeft: 15}}
                                color="primary"
                                aria-label="upload picture"
                                component="span">
                                <CachedIcon/>
                            </IconButton>
                            <Popover
                                open={Boolean(anchorEl)}
                                anchorEl={anchorEl}
                                onClose={handleClose}
                                anchorOrigin={{
                                    vertical: "bottom",
                                    horizontal: "center"
                                }}
                                transformOrigin={{
                                    vertical: "top",
                                    horizontal: "center"
                                }}
                            >
                                <div className={classes.pop}>
                                    <Alert severity="warning">
                                        <AlertTitle>APIToken 刷新后，通过 API
                                            访问 {configs.app} 后台的请求将会被阻断</AlertTitle>
                                        如果 APIToken 没有泄漏，不建议重制 Token
                                    </Alert>
                                    <div className={classes.popBtns}>
                                        <Button
                                            onClick={handleClose}
                                            color="primary"
                                            aria-label="upload picture"
                                            component="span">
                                            取消
                                        </Button>
                                        <Button
                                            onClick={() => {
                                                doRefreshApiToken();
                                                handleClose();
                                            }}
                                            style={{marginLeft: 15}}
                                            color="primary"
                                            aria-label="upload picture"
                                            component="span">
                                            确定
                                        </Button>
                                    </div>
                                </div>
                            </Popover>
                        </Typography>
                    </Grid>
                </Grid>
            </CardContent>
        </Card>
    );
};

export default UserDashboard;
