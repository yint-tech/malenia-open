import React, {useContext} from "react";
import {createUseStyles, useTheme} from "react-jss";
import {SearchInput} from 'components';
import {AppContext} from "adapter";
import moment from "moment";
import clsx from 'clsx';
import apis from "apis";

import config from "config";
import {withRouter} from "react-router-dom";
import {Button, TextField} from "@mui/material";

const useStyles = createUseStyles({
    root: {},
    row: {
        height: '42px',
        display: 'flex',
        alignItems: 'center',
        marginTop: ({theme}) => theme.spacing(1)
    },
    spacer: {
        flexGrow: 1
    },
    importButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
    exportButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
    searchInput: {},
    dialog: {
        width: ({theme}) => theme.spacing(60)
    },
    dialogInput: {
        width: '100%'
    },
    ml: {
        marginLeft: ({theme}) => theme.spacing(2)
    },
});

const Toolbar = props => {
    const {
        className, onInputChange,
        startDate = moment().subtract(3, 'months').format("yyyy-MM-DD HH:mm:ss"),
        setStartDate = () => {
        },
        endDate = moment().format("yyyy-MM-DD HH:mm:ss"),
        setEndDate = () => {
        },
        ...rest
    } = props;
    const {user} = useContext(AppContext);
    const theme = useTheme();
    const classes = useStyles({theme});

    return (
        <div
            {...rest}
            className={clsx(classes.root, className)}
        >
            <div className={classes.row}>
                <SearchInput
                    className={classes.searchInput}
                    onChange={(v) => onInputChange(v)}
                    placeholder="请输入关键词进行查询"
                />
                <span className={classes.spacer}/>
                <TextField
                    type="datetime-local"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value ?
                        moment(new Date(e.target.value)).format("YYYY-MM-DD HH:mm:ss") :
                        ""
                    )}
                    InputLabelProps={{
                        shrink: true,
                    }}
                />
                <span style={{margin: '0 10px'}}> ~ </span>
                <TextField
                    type="datetime-local"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value ?
                        moment(new Date(e.target.value)).format("YYYY-MM-DD HH:mm:ss") :
                        "")}
                    InputLabelProps={{
                        shrink: true,
                    }}
                />
                <span className={classes.spacer}/>
                <Button
                    style={{marginRight: 10}}
                    color="primary"
                    size="medium"
                    variant="contained"
                    onClick={() => {
                        let url = `${apis.urls['exportRechargeRecord']}?${config.login_token_key}=${user.loginToken}&startTime=${startDate}&endTime=${endDate}`;
                        window.open(url);
                    }}>导出 EXCEL</Button>
            </div>
        </div>
    );
};

export default withRouter(Toolbar);
