import React, {useContext, useEffect, useState} from 'react';
import {createUseStyles, useTheme} from "react-jss";

import {Table, Toolbar} from './components';
import {AppContext} from 'adapter';
import {Button, Card, Typography} from "@mui/material";
import {withRouter} from "react-router-dom";

const useStyles = createUseStyles({
    root: {
        padding: ({theme}) => theme.spacing(3)
    },
    content: {
        marginTop: ({theme}) => theme.spacing(2)
    },
    tags: {
        marginTop: ({theme}) => theme.spacing(2),
        marginBottom: ({theme}) => theme.spacing(2),
        paddingLeft: ({theme}) => theme.spacing(2),
        paddingRight: ({theme}) => theme.spacing(2),
        paddingTop: ({theme}) => theme.spacing(1),
        paddingBottom: ({theme}) => theme.spacing(1)
    },
    tagButton: {
        border: '1px dashed #f0f0f0',
        marginRight: ({theme}) => theme.spacing(1)
    },
    tagButtonActive: {
        border: '1px dashed #2196f3',
        backgroundColor: '#2196f3',
        marginRight: ({theme}) => theme.spacing(1)
    }
});

const P = () => {
    const theme = useTheme();
    const classes = useStyles({theme});

    const {user, api} = useContext(AppContext);
    const [products, setProducts] = useState([]);
    const [buys, setBuys] = useState([]);
    const [page, setPage] = useState(1);
    const [limit] = useState(10);
    const [keyword, setKeyword] = useState('');
    const [refresh, setRefresh] = useState(+new Date());

    const [tags, setTags] = useState([]);
    const [filters, setFilters] = useState([]);

    useEffect(() => {
        if (!user.apiToken) return
        api.listAllProducts().then(res => {
            if (res.status === 0) {
                let temp = res.data.filter(item => {
                    return item.enabled
                });
                setProducts(temp);
                // 获取 tags
                let t = [];
                temp.map(item => {
                    t = t.concat(item.features.split(','));
                    return item;
                });
                setTags(t);
                setFilters(t);
            }
        })
        api.listOrder().then(res => {
            if (res.status === 0) {
                setBuys(res.data.map(item => item.productId));
            }
        })
    }, [api, refresh, user]);

    const showData = products.filter(item => {
        let features = item.features?.split(',') || [];
        let hasTag = false;
        for (let i of features) {
            if (filters.includes(i)) {
                hasTag = true;
            }
        }
        if (!hasTag) return false;
        return JSON.stringify(item).includes(keyword);
    });

    return (
        <div className={classes.root}>
            <Toolbar onInputChange={(k) => {
                setKeyword(k);
                setPage(1);
            }} setRefresh={setRefresh}/>
            <Card className={classes.tags}>
                {tags.map(item => (
                    <Button
                        key={item}
                        size="small"
                        onClick={() => {
                            if (filters.includes(item)) {
                                setFilters([...filters].filter(f => f !== item));
                            } else {
                                setFilters([...filters, item]);
                            }
                        }}
                        className={filters.includes(item) ? classes.tagButtonActive : classes.tagButton}>
                        <Typography variant="subtitle2" style={{color: filters.includes(item) ? '#fff' : '#546e7a'}}>
                            {item}
                        </Typography>
                    </Button>
                ))}
            </Card>
            <div className={classes.content}>
                <Table
                    data={showData.slice((page - 1) * limit, page * limit)}
                    total={showData.length}
                    rowsPerPage={limit}
                    pageState={[page, setPage]}
                    setRefresh={setRefresh}
                    buys={buys}/>
            </div>
        </div>
    );
};

export default withRouter(P);
