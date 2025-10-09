import IconButton from '@mui/material/IconButton';
import SnackbarContent from '@mui/material/SnackbarContent';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CloseIcon from '@mui/icons-material/Close';
import ErrorIcon from '@mui/icons-material/Error';
import InfoIcon from '@mui/icons-material/Info';
import WarningIcon from '@mui/icons-material/Warning';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import * as React from 'react';
import {createUseStyles, useTheme} from "react-jss";

const variantIcon = {
    success: CheckCircleIcon,
    warning: WarningIcon,
    error: ErrorIcon,
    info: InfoIcon,
};

const useStyles = createUseStyles({
    successAlert: {
        backgroundColor: ({theme}) => theme.palette.success.main,
    },
    errorAlert: {
        backgroundColor: ({theme}) => theme.palette.error.main,
    },
    infoAlert: {
        backgroundColor: ({theme}) => theme.palette.info.main,
    },
    warningAlert: {
        backgroundColor: ({theme}) => theme.palette.warning.main,
    },
    message: {
        display: 'flex',
        alignItems: 'center',
        '& > svg': {
            marginRight: ({theme}) => theme.spacing(1),
        },
    },
    icon: {
        fontSize: 20,
        opacity: 0.9,
    },
    closeButton: {},
});

function SnackbarContentWrapper(props) {
    const {message, onClose, variant, ...other} = props;
    const Icon = variantIcon[variant];
    const theme = useTheme();
    const classes = useStyles({theme});
    return (
        <SnackbarContent
            className={clsx(classes[`${variant}Alert`])}
            aria-describedby="client-snackbar"
            message={
                <span id="client-snackbar" className={classes.message}>
                    <Icon className={classes.icon}/>
                    {message}
                </span>
            }
            action={[
                <IconButton
                    key="close"
                    aria-label="Close"
                    color="inherit"
                    className={classes.closeButton}
                    onClick={onClose}
                >
                    <CloseIcon className={classes.icon}/>
                </IconButton>,
            ]}
            {...other}
        />
    );
}

SnackbarContentWrapper.propTypes = {
    message: PropTypes.node,
    onClose: PropTypes.func,
    variant: PropTypes.oneOf(['success', 'warning', 'error', 'info']).isRequired,
};

export default SnackbarContentWrapper;
