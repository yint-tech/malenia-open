/* eslint-disable react/no-multi-comp */
/* eslint-disable react/display-name */
import React, {forwardRef} from 'react';
import {NavLink as RouterLink} from 'react-router-dom';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import {Button, colors, List, ListItem} from '@mui/material';
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {},
    item: {
        display: 'flex',
        paddingTop: 0,
        paddingBottom: 0
    },
    button: {
        color: colors.blueGrey[800],
        padding: '10px 8px',
        justifyContent: 'flex-start',
        textTransform: 'none',
        letterSpacing: 0,
        width: '100%',
        fontWeight: ({theme}) => theme.typography.fontWeightMedium
    },
    icon: {
        color: ({theme}) => theme.palette.icon,
        width: 24,
        height: 24,
        display: 'flex',
        alignItems: 'center',
        marginRight: ({theme}) => theme.spacing(1)
    },
    active: {
        color: ({theme}) => theme.palette.primary.main,
        fontWeight: ({theme}) => theme.typography.fontWeightMedium,
        '& $icon': {
            color: ({theme}) => theme.palette.primary.main
        }
    }
});

const CustomRouterLink = forwardRef((props, ref) => (
    <div
        ref={ref}
        style={{flexGrow: 1}}
    >
        <RouterLink {...props} />
    </div>
));

const SidebarNav = props => {
    const {pages, className, ...rest} = props;

    const theme = useTheme();
    const classes = useStyles({theme});

    return (
        <List
            {...rest}
            className={clsx(classes.root, className)}
        >
            {pages.map(page => (
                <ListItem
                    className={classes.item}
                    disableGutters
                    key={page.title}
                    divider={!!page.divider}
                >
                    <Button
                        activeClassName={classes.active}
                        className={classes.button}
                        component={CustomRouterLink}
                        to={page.href}
                    >
                        <div className={classes.icon}>{page.icon}</div>
                        {page.title}
                    </Button>
                </ListItem>
            ))}
        </List>
    );
};

SidebarNav.propTypes = {
    className: PropTypes.string,
    pages: PropTypes.array.isRequired
};

export default SidebarNav;
