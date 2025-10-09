import React, {useCallback, useEffect, useState} from "react";

import clsx from "clsx";
import {Card, CardActions, CardContent, CardHeader, Input, Pagination} from "@mui/material";
import Table from "../Table";
import PropTypes from "prop-types";
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    row: {
        height: '100%',
        display: "flex",
        alignItems: "center",
        flexWrap: "wrap",
        marginTop: ({theme}) => theme.spacing(1)
    },
    spacer: {
        flexGrow: 1
    },
    tableRoot: {},
    tableContent: {
        padding: 0
    },
    searchInput: {
        marginBottom: ({theme}) => theme.spacing(2)
    },
    actions: {
        paddingTop: ({theme}) => theme.spacing(2),
        paddingBottom: ({theme}) => theme.spacing(2),
        justifyContent: "center"
    },
});

const DataTable = props => {
    const {
        className, data, total, page, pageSize, loading, pageChangeFunc, renderCollapse,
        columns, title, ...rest
    } = props;

    const theme = useTheme();
    const classes = useStyles({theme});

    return (
        <Card
            className={clsx(classes.tableRoot, className)}
        >
            {title && <CardHeader title={title}/>}
            <CardContent className={classes.tableContent}>
                <Table
                    {...rest}
                    loading={loading}
                    collapse={!!renderCollapse}
                    renderCollapse={renderCollapse}
                    data={data}
                    columns={columns}
                />
            </CardContent>
            <CardActions className={classes.actions}>
                <Pagination
                    count={Math.ceil(total / pageSize) || 1}
                    page={page}
                    onChange={pageChangeFunc}
                    shape="rounded"/>
                <Input
                    type="number"
                    value={page}
                    onChange={(e) => {
                        let max = Math.ceil(total / pageSize) || 1;
                        if (e.target.value > max) {
                            pageChangeFunc(null, max);
                        } else if (e.target.value < 1) {
                            pageChangeFunc(null, 1);
                        } else {
                            pageChangeFunc(null, e.target.value);
                        }
                    }}
                />
            </CardActions>
        </Card>
    );
};

const BackendPagedTable = (props) => {
    const theme = useTheme();
    const classes = useStyles({theme});

    const [page, setPage] = useState(1);
    const defaultSearchParamBuilder = useCallback((param) => param, [])

    const {
        loadDataFun,
        searchParam,
        toolbar,
        columns,
        refresh,
        renderCollapse,
        searchParamBuilder = defaultSearchParamBuilder,
        ...rest
    } = props;

    const [loading, setLoading] = useState(false);
    const [records, setRecords] = useState([]);
    const [total, setTotal] = useState(0);

    const [innerRefresh, setInnerRefresh] = useState(refresh || +new Date());

    useEffect(() => {
        setLoading(true);
        const param = searchParamBuilder(searchParam);
        loadDataFun({
            ...param,
            page: page,
            pageSize: 10
        }).then(res => {
            if (res.status === 0) {
                setRecords(res.data.records);
                setTotal(res.data.total);
            }
        }).finally(() => {
            setLoading(false);
        });
    }, [loadDataFun, innerRefresh, searchParam, searchParamBuilder]);

    useEffect(() => {
        setInnerRefresh(+new Date())
    }, [refresh])

    const handlePageChange = (event, page) => {
        setPage(page)
        setInnerRefresh(+new Date());
    };

    return (
        <div className={classes.root}>
            {
                toolbar ? <div className={classes.row}>{
                    toolbar
                }</div> : <></>
            }
            <div className={classes.content}>
                <DataTable
                    {...rest}
                    renderCollapse={renderCollapse}
                    pageChangeFunc={handlePageChange}
                    loading={loading}
                    data={records}
                    columns={columns}
                    total={total}
                    page={page}
                    pageSize={10}
                    setRefresh={setInnerRefresh}/>
            </div>
        </div>
    );
};

BackendPagedTable.propTypes = {
    loadDataFun: PropTypes.func.isRequired,
    columns: PropTypes.array.isRequired,
    searchParam: PropTypes.object.isRequired,
    toolbar: PropTypes.element,
    refresh: PropTypes.number,
    renderCollapse: PropTypes.func
};

export default BackendPagedTable;
