import React, {useContext} from "react";
import clsx from "clsx";
import PropTypes from "prop-types";
import {
    AccountBox,
    Equalizer,
    Extension,
    Home,
    SettingsApplications,
    ShowChart,
    VpnLock,
    WbSunny
} from "@mui/icons-material"
import {AppContext} from "adapter";
import {Divider, Drawer} from "@mui/material";

import {Profile, SidebarNav} from "./components";
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    drawer: ({theme}) => ({
        width: 240,
        [theme.breakpoints.up("lg")]: {
            marginTop: 64,
            height: "calc(100% - 64px)"
        }
    }),
    root: {
        backgroundColor: ({theme}) => theme.palette.white,
        display: "flex",
        flexDirection: "column",
        height: "100%",
        padding: ({theme}) => theme.spacing(2)
    },
    divider: {
        margin: ({theme}) => theme.spacing(2, 0)
    },
    nav: {
        marginBottom: ({theme}) => theme.spacing(2)
    }
});

const Sidebar = props => {
    const {user} = useContext(AppContext);
    const {open, variant, onClose, className, ...rest} = props;

    const theme = useTheme();
    const classes = useStyles({theme});

    let pages = [
        {
            title: "我的",
            href: "/mine",
            icon: <Home/>
        },
        {
            title: "代理产品",
            href: "/proxy",
            icon: <VpnLock/>
        }, {
            title: "流量编辑",
            href: "/mitm",
            icon: <Extension/>
        }

    ];

    if (user.isAdmin) {
        pages[pages.length - 1].divider = true;
        pages = pages.concat([
            {
                title: "IP资源",
                href: "/ipSource",
                icon: <WbSunny/>
            }, {
                title: "产品管理",
                href: "/product",
                icon: <Equalizer/>
            },
            {
                title: "监控指标",
                href: "/metrics",
                icon: <ShowChart/>
            },
            {
                title: "账号列表",
                href: "/accountList",
                icon: <AccountBox/>
            }, {
                title: "系统面板",
                href: "/systemSettings",
                icon: <SettingsApplications/>
            }]);
    }

    return (
        <Drawer
            anchor="left"
            classes={{paper: classes.drawer}}
            onClose={onClose}
            open={open}
            variant={variant}
        >
            <div
                {...rest}
                className={clsx(classes.root, className)}
            >
                <Profile/>
                <Divider className={classes.divider}/>
                <SidebarNav
                    className={classes.nav}
                    pages={pages}
                />
            </div>
        </Drawer>
    );
};

Sidebar.propTypes = {
    className: PropTypes.string,
    onClose: PropTypes.func,
    open: PropTypes.bool.isRequired,
    variant: PropTypes.string.isRequired
};

export default Sidebar;
