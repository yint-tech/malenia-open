import React, {useContext, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle} from '@mui/material';
import Loading from '../Loading';
import PropTypes from "prop-types";
import {AppContext} from "adapter";
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    dialog: {
        minWidth: ({theme}) => theme.spacing(70)
    },
});

const OpeDialog = props => {
    const {
        title,
        opeText,
        opeContent,
        openDialog,
        setOpenDialog,
        doDialog,
        okText,
        okType,
        ...rest
    } = props;

    const [loading, setLoading] = useState(false);

    const {api} = useContext(AppContext);

    const theme = useTheme();
    const classes = useStyles({theme});

    return (
        <Dialog
            {...rest}
            onClose={() => setOpenDialog(false)} open={openDialog}>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent className={classes.dialog}>
                {opeContent ? opeContent : (loading ? (
                    <Loading/>
                ) : (
                    <DialogContentText>
                        {opeText}
                    </DialogContentText>
                ))}
            </DialogContent>
            <DialogActions>
                <Button onClick={() => setOpenDialog(false)} color="primary">
                    取消
                </Button>
                <Button onClick={() => {
                    if (!doDialog) {
                        setOpenDialog(false);
                        return
                    }
                    setLoading(true);
                    let dialogActionRet = doDialog();
                    if (!dialogActionRet) {
                        return;
                    }
                    if (dialogActionRet['then'] && typeof dialogActionRet['then'] === 'function') {
                        dialogActionRet.then((message) => {
                            if (message && typeof message === 'string') {
                                api.successToast(message);
                            }
                            let failed = typeof message === 'boolean' && !message;

                            if (!failed && typeof message === 'object') {
                                failed = (typeof message.status) !== undefined
                                    && message.status !== 0;
                            }

                            if (!failed) {
                                setOpenDialog(false);
                            }
                        }).catch((e) => {
                            console.error(e)
                            api.errorToast(e.message);
                        }).finally(() => {
                            setLoading(false);
                        });
                    } else {
                        setOpenDialog(false);
                    }
                }} color={okType || "secondary"} autoFocus>
                    {okText || "确认"}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

OpeDialog.propTypes = {
    title: PropTypes.string.isRequired,
    opeText: PropTypes.string,
    opeContent: PropTypes.element,
    openDialog: PropTypes.bool.isRequired,
    setOpenDialog: PropTypes.func.isRequired,
    okText: PropTypes.string,
    okType: PropTypes.oneOf(['inherit', 'primary', 'secondary', 'default'])
};

export default OpeDialog;
