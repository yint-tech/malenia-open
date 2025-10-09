import React from 'react';
import {createUseStyles, useTheme} from "react-jss";
import {Grid} from "@mui/material";
import {withRouter} from "react-router-dom";

const useStyles = createUseStyles({
    logPanel: {
        maxWidth: '75%'
    },
    pre: {
        width: '100%',
        overflow: 'auto',
        background: '#f1f1f1',
        borderRadius: ({theme}) => theme.spacing(1),
        maxHeight: ({theme}) => theme.spacing(100),
        padding: ({theme}) => theme.spacing(2),
        whiteSpace: 'pre-wrap',
        boxSizing: 'border-box'
    },
    listText: {
        cursor: 'pointer'
    }
});

const Logs = () => {

    return (
        <Grid
            container
            spacing={6}
            wrap="wrap"
        >
            <Grid item>
                <div>todo</div>
            </Grid>

        </Grid>
    );
};

export default withRouter(Logs);

