import React, {useContext, useEffect, useState} from 'react';
import {createUseStyles, useTheme} from "react-jss";
import {Table, Toolbar} from './components';
import {AppContext} from "adapter";
import moment from "moment";
import {Grid} from "@mui/material";
import {withRouter} from "react-router-dom";

const useStyles = createUseStyles({
    root: {
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    }
});

const RechargeRecord = () => {
    const theme = useTheme();
    const classes = useStyles({theme});
    const {api} = useContext(AppContext);

    const [keyword, setKeyword] = useState('');
    const [limit] = useState(10);
    const [page, setPage] = useState(1);
    const [refresh] = useState(+new Date());

    const [rechargeList, setRechargeList] = useState([]);

    const [startDate, setStartDate] = useState(moment().subtract(3, 'months').format("yyyy-MM-DD HH:mm:ss"));
    const [endDate, setEndDate] = useState(moment().format("yyyy-MM-DD HH:mm:ss"));


    useEffect(() => {
        api.listRechargeRecords({
            page: 1,
            pageSize: 1000,
            startTime: startDate,
            endTime: endDate,

        }).then(res => {
            if (res.status === 0) {
                setRechargeList(res.data.records || []);
            }
        })
    }, [refresh, api, startDate, endDate]);

    const showTable1 = rechargeList.filter(item => {
        return JSON.stringify(item).includes(keyword);
    });

    return (
        <div className={classes.root}>
            <Toolbar
                onInputChange={(k) => {
                    setKeyword(k);
                    setPage(1);
                }}
                startDate={startDate}
                setStartDate={setStartDate}
                endDate={endDate}
                setEndDate={setEndDate}/>

            <div className={classes.content}>
                <Grid
                    container
                    spacing={4}
                >
                    <Grid
                        item
                        sm={12}
                        xs={12}
                    >
                        <Table
                            data={showTable1.slice((page - 1) * limit, page * limit)}
                            total={showTable1.length}
                            rowsPerPage={limit}
                            pageState={[page, setPage]}/>
                    </Grid>
                </Grid>
            </div>
        </div>
    );
};

export default withRouter(RechargeRecord);
