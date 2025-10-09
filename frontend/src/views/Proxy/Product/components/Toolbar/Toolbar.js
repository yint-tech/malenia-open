import React from 'react';
import {createUseStyles, useTheme} from "react-jss";
import {SearchInput} from 'components';

import clsx from 'clsx';

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
    searchInput: {}
});

const Toolbar = props => {
    const {className, onInputChange, setRefresh, ...rest} = props;
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
            </div>
        </div>
    );
};

export default Toolbar;
