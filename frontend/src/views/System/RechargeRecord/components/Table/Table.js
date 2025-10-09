import React from "react";
import clsx from "clsx";
import PropTypes from "prop-types";
import {createUseStyles, useTheme} from "react-jss";
import {Table} from "components";
import moment from "moment";
import {Card, CardActions, CardContent, Pagination} from "@mui/material";
import {withRouter} from "react-router-dom";

const useStyles = createUseStyles({
    root: {},
    content: {
        padding: 0
    },
    nameContainer: {
        display: "flex",
        alignItems: "center"
    },
    avatar: {
        marginRight: ({theme}) => theme.spacing(2)
    },
    actions: {
        paddingTop: ({theme}) => theme.spacing(2),
        paddingBottom: ({theme}) => theme.spacing(2),
        justifyContent: "center"
    },
    tableButton: {
        marginRight: ({theme}) => theme.spacing(1)
    }
});

const DataTable = props => {
    const {className, data, total, rowsPerPage, pageState, ...rest} = props;
    const [page, setPage] = pageState;

    const theme = useTheme();
    const classes = useStyles({theme});

    const handlePageChange = (event, page) => {
        setPage(page);
    };

    return (
        <Card
            {...rest}
            className={clsx(classes.root, className)}
        >
            <CardContent className={classes.content}>
                <Table
                    data={data}
                    columns={[
                        {
                            label: "ID",
                            key: "id"
                        }, {
                            label: "操作人",
                            key: "operator"
                        }, {
                            label: "充值用户",
                            key: "user"
                        }, {
                            label: "到账金额",
                            key: "rechargeAmount"
                        }, {
                            label: "实际充值金额",
                            key: "actualPayAmount"
                        }, {
                            label: "充值后余额",
                            key: "remainBalance"
                        }, {
                            label: "充值实际总额",
                            key: "totalActualPayAmount"
                        }, {
                            label: "充值时间",
                            render: (item) => moment(new Date(item.createTime)).format("YYYY-MM-DD HH:mm:ss")
                        },
                        {
                            label: "备注",
                            key: "comment"
                        }
                    ]}
                />
            </CardContent>
            <CardActions className={classes.actions}>
                <Pagination
                    count={Math.ceil(total / rowsPerPage) || 1}
                    page={page}
                    onChange={handlePageChange}
                    shape="rounded"/>
            </CardActions>
        </Card>
    );
};

DataTable.propTypes = {
    className: PropTypes.string,
    data: PropTypes.array.isRequired
};

export default withRouter(DataTable);
