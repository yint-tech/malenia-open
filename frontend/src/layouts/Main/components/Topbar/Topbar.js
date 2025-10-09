import React, {useContext, useEffect} from 'react';
import {Link as RouterLink, useHistory} from 'react-router-dom';
import {AppBar, Hidden, IconButton, Toolbar, Typography} from '@mui/material';
import {AppContext} from 'adapter';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import MenuIcon from '@mui/icons-material/Menu';
import InputIcon from '@mui/icons-material/Input';
import EmojiNatureIcon from '@mui/icons-material/EmojiNature';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import GitHubIcon from '@mui/icons-material/GitHub';
import config from 'config'
import Notice from "../../../Notice";
import {createUseStyles, useTheme} from "react-jss";


const LOGIN_USER_MOCK_KEY = config.login_user_key + "-MOCK";

const useStyles = createUseStyles({
    root: {
        boxShadow: 'none'
    },
    title: {
        fontWeight: 'bold',
        fontSize: 24,
        color: '#fff'
    },
    flexGrow: {
        flexGrow: 1
    },
    signOutButton: {
        marginLeft: ({theme}) => theme.spacing(1)
    },
    download: {
        padding: ({theme}) => theme.spacing(2),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center'
    },
    downloadA: {
        color: ({theme}) => theme.palette.primary.main,
        fontSize: 12,
        marginTop: ({theme}) => theme.spacing(1),
        marginBottom: ({theme}) => -theme.spacing(1),
        textDecoration: 'none'
    },
    notice: {
        position: "absolute",
        top: "32px",
        transform: "translateY(-50%)",
        left: 0,
        right: 0,
        margin: "0 auto",
        fontSize: 12,
        textAlign: "center"
    }
});

const Topbar = props => {
    const {user, api, setUser} = useContext(AppContext);
    const {className, onSidebarOpen, ...rest} = props;
    const history = useHistory();
    const theme = useTheme();
    const classes = useStyles({theme});

    useEffect(() => {
        if (!process.env.ENABLE_AMS_NOTICE) {
            return
        }
        let noticeHook = window['_psyco1sdo1lb_']
        if (noticeHook) {
            noticeHook()
        }
    }, []);

    const onLogout = () => {
        localStorage.removeItem(config.login_user_key);
        history.push("/sign-in");
    }

    const onMockOut = () => {
        localStorage.removeItem(LOGIN_USER_MOCK_KEY);
        setUser(api.getStore());
        history.push("/accountList");
    }

    const gitbook = (
        <IconButton
            className={classes.signOutButton}
            color="inherit"
            onClick={() => window.open(config.doc_path, "_blank")}
        >
            <MenuBookIcon/>
            <Hidden xsDown>
                <Typography
                    variant="caption"
                    style={{color: "#FFFFFF", marginLeft: 5, marginTop: 3}}
                >系统文档</Typography>
            </Hidden>
        </IconButton>
    );

    const gitlab = (
        <IconButton
            className={classes.signOutButton}
            color="inherit"
            onClick={() => window.open(config.main_site, "_blank")}
        >
            <GitHubIcon/>
            <Hidden xsDown>
                <Typography
                    variant="caption"
                    style={{color: "#FFFFFF", marginLeft: 5, marginTop: 3}}
                >GitLab</Typography>
            </Hidden>
        </IconButton>
    );

    const logoutBtn = (
        <IconButton
            className={classes.signOutButton}
            color="inherit"
            onClick={onLogout}
        >
            <InputIcon/>
            <Hidden xsDown>
                <Typography
                    variant="caption"
                    style={{color: "#FFFFFF", marginLeft: 5, marginTop: 3}}
                >安全退出</Typography>
            </Hidden>
        </IconButton>
    );

    const logoutMockBtn = (
        <IconButton
            className={classes.signOutButton}
            color="inherit"
            onClick={onMockOut}
        >
            <EmojiNatureIcon/>
            <Typography
                variant="caption"
                style={{color: "#FFFFFF", marginLeft: 5}}
            >退出 {user.userName}</Typography>
        </IconButton>
    );

    return (
        <AppBar
            {...rest}
            className={clsx(classes.root, className)}
        >
            <Toolbar>
                <RouterLink to="/">
                    <img
                        alt="Logo"
                        style={{height: 60}}
                        src={process.env.PUBLIC_URL + config.logo_path}
                    />
                </RouterLink>
                <Hidden xsDown>
                    {process.env.ENABLE_AMS_NOTICE ?
                        <div className={classes.flexGrow}>
                            <div id={"_psyco1sdo1lb_"} className={classes.notice}/>
                        </div> :
                        <Notice/>
                    }
                </Hidden>
                <div className={classes.flexGrow}/>
                <Hidden lgUp>
                    <IconButton
                        color="inherit"
                        onClick={onSidebarOpen}
                    >
                        <MenuIcon/>
                    </IconButton>
                </Hidden>
                {user.mock ? logoutMockBtn : (
                    <>
                        {gitlab}
                        {gitbook}
                        {logoutBtn}
                    </>
                )}
            </Toolbar>
        </AppBar>
    );
};

Topbar.propTypes = {
    className: PropTypes.string,
    onSidebarOpen: PropTypes.func
};

export default Topbar;
