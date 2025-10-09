import React from 'react';
import PropTypes from 'prop-types';
import {Topbar} from './components';
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {
        paddingTop: 64,
        height: '100%'
    },
    content: {
        height: '100%'
    }
});

const Minimal = props => {
    const {children} = props;

    const theme = useTheme();
    const classes = useStyles({theme});

    return (
        <div className={classes.root}>
            <Topbar/>
            <main className={classes.content}>{children}</main>
        </div>
    );
};

Minimal.propTypes = {
    children: PropTypes.node,
    className: PropTypes.string
};

export default Minimal;
