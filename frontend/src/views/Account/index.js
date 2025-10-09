import React, {useCallback, useContext, useState} from 'react';
import {OpeDialog, SimpleTable} from "components";
import {AppContext} from "adapter";
import {Button, Grid, TextField, Typography} from "@mui/material";
import {AssignmentInd, AttachMoney, DirectionsRailway, PermIdentity, SupervisorAccount} from "@mui/icons-material";
import config from "../../config";
import moment from "moment/moment";
import {useHistory} from "react-router-dom";
import EmojiPeopleIcon from "@mui/icons-material/EmojiPeople";
import Permission from "./Permission";
import {createUseStyles, useTheme} from "react-jss";
import AuthenticationDialog from "components/AuthenticationDialog";

const LOGIN_USER_MOCK_KEY = config.login_user_key + "-MOCK";

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
    },
    dialogInput: {
        width: "100%"
    }
});

const CreateUserDialog = (props) => {
    const {openCreateUserDialog, setOpenCreateUserDialog, setRefresh} = props;
    const theme = useTheme();
    const classes = useStyles({theme});
    const {api} = useContext(AppContext);
    const [account, setAccount] = useState("");
    const [password, setPassword] = useState("");

    return (<OpeDialog
        title="添加用户"
        opeContent={(
            <>
                <Grid
                    container
                    spacing={6}
                    wrap="wrap"
                >
                    <Grid
                        item
                        xs={6}
                    >
                        <Typography
                            gutterBottom
                            variant="h6"
                        >
                            账号
                        </Typography>
                        <TextField
                            className={classes.dialogInput}
                            size="small"
                            variant="outlined"
                            value={account}
                            onChange={(e) => setAccount(e.target.value)}/>
                    </Grid>
                    <Grid
                        item
                        xs={6}
                    >
                        <Typography
                            gutterBottom
                            variant="h6"
                        >
                            密码
                        </Typography>
                        <TextField
                            className={classes.dialogInput}
                            size="small"
                            variant="outlined"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}/>
                    </Grid>
                </Grid>
            </>
        )}
        openDialog={openCreateUserDialog}
        setOpenDialog={setOpenCreateUserDialog}
        doDialog={() => {
            return api.userAdd({
                userName: account,
                password: password
            }).then(res => {
                if (res.status === 0) {
                    setRefresh(+new Date());
                }
            });
        }}
        okText="保存"
        okType="primary"/>);
}


const RechargeDialog = (props) => {
    const {openRechargeDialog, setOpenRechargeDialog, setRefresh, chargeTargetUser} = props;
    const [chargeData, setChargerData] = useState({
        amount: '',
        actualAmount: '',
        rechargeComment: ''
    })

    const theme = useTheme();
    const classes = useStyles({theme});
    const {api} = useContext(AppContext);
    return (<OpeDialog
        title="余额充值"
        opeContent={(
            <>
                <Typography variant="h6">
                    充值金额
                </Typography>
                <TextField
                    className={classes.dialogInput}
                    size="small"
                    variant="outlined"
                    type="number"
                    placeholder="请输入充值金额"
                    value={chargeData.amount}
                    onChange={(e) => {
                        setChargerData({
                            ...chargeData,
                            amount: e.target.value,
                            actualAmount: e.target.value
                        })
                    }}/>
                <Typography variant="h6" style={{marginTop: 10}}>
                    实际到账金额
                </Typography>
                <TextField
                    className={classes.dialogInput}
                    size="small"
                    variant="outlined"
                    type="number"
                    placeholder="请输入实际到账金额"
                    value={chargeData.actualAmount}
                    onChange={(e) => {
                        setChargerData({
                            ...chargeData,
                            actualAmount: e.target.value
                        })
                    }}/>
                <Typography variant="h6" style={{marginTop: 10}}>
                    充值备注
                </Typography>
                <TextField
                    className={classes.dialogInput}
                    size="small"
                    variant="outlined"
                    placeholder="请输入备注"
                    value={chargeData.rechargeComment}
                    onChange={(e) => {
                        setChargerData({
                            ...chargeData,
                            rechargeComment: e.target.value,
                        })
                    }}/>
            </>
        )}
        openDialog={openRechargeDialog}
        setOpenDialog={setOpenRechargeDialog}
        doDialog={() => {
            return api.rechargeUser({
                user: chargeTargetUser.userName,
                amount: chargeData.amount,
                actualPayAmount: chargeData.actualAmount,
                rechargeComment: chargeData.rechargeComment
            }).then(res => {
                if (res.status === 0) {
                    setRefresh(+new Date());
                    return '操作成功';
                }
                throw new Error(res.message);
            });
        }}
        okText="保存"
        okType="primary"/>);
}


const AccountList = () => {

    const {api, setUser,systemInfo} = useContext(AppContext);
    const history = useHistory();
    const theme = useTheme();
    const classes = useStyles({theme});

    const [openCreateUserDialog, setOpenCreateUserDialog] = useState(false);

    const [permOpAccount, setPermOpAccount] = useState({});
    const [showPermOpDialog, setShowPermOpDialog] = useState(false);
    const [refresh, setRefresh] = useState(+new Date());


    const [chargeTargetUser, setChargeTargetUser] = useState({});
    const [authTargetUser, setAuthTargetUser] = useState({});
    const [openRechargeDialog, setOpenRechargeDialog] = useState(false);
    const [openAuthDialog, setOpenAuthDialog] = useState(false);


    const travelToUser = (item) => {
        api.travelToUser({
            id: item.id
        }).then(res => {
            if (res.status === 0) {
                api.setStore({...res.data, mock: true}, LOGIN_USER_MOCK_KEY);
                setUser({
                    ...res.data,
                    mock: true,
                    time: moment(new Date()).format("YYYY-MM-DD HH:mm:ss")
                });
                history.push("/");
            }
        });
    };

    const grantAdmin = (item) => {
        api.grantAdmin({
            userName: item.userName,
            isAdmin: !item.isAdmin
        }).then(res => {
            if (res.status === 0) {
                setRefresh(+new Date());
            }
        });
    };


    const loadApi = useCallback(() => {
        return new Promise((resolve, reject) => {
            api.userList({page: 1, pageSize: 1000})
                .then(res => {
                    if (res.status === 0) {
                        resolve({
                            data: res.data.records,
                            status: 0
                        });
                        return
                    }
                    reject(res.message)
                })
                .catch((e) => {
                    reject(e)
                })
            ;
        })
    }, [api])

    return (
        <div>
            <SimpleTable
                refresh={refresh}
                actionEl={(<Button
                    startIcon={<EmojiPeopleIcon/>}
                    color="primary"
                    variant="contained"
                    onClick={() => setOpenCreateUserDialog(true)}
                >
                    添加用户
                </Button>)}
                loadDataFun={loadApi}
                renderCollapse={(item) => (
                    <Grid
                        container
                        spacing={6}
                        wrap="wrap"
                    >
                        <Grid item xs={12}>
                            <Typography gutterBottom variant="h6">
                                代理账户：{item.authAccount}
                            </Typography>
                            <Typography gutterBottom variant="h6">
                                代理密码：{item.authPwd}
                            </Typography>
                        </Grid>
                        {
                            systemInfo.buildInfo.supportCertification ? (<Grid item xs={12}>
                                <Typography gutterBottom variant="h6">
                                    个人认证-姓名：{item.authenticationRealName}
                                </Typography>
                                <Typography gutterBottom variant="h6">
                                    个人认证-身份证：{item.authenticationIdCard}
                                </Typography>
                                <Typography gutterBottom variant="h6">
                                    企业认证-公司名称：{item.authenticationCompanyName}
                                </Typography>
                                <Typography gutterBottom variant="h6">
                                    企业认证-统一社会信用代码：{item.authenticationLicenseNumber}
                                </Typography>
                                <Typography gutterBottom variant="h6">
                                    企业认证-法人姓名：{item.authenticationCorporateName}
                                </Typography>
                                <Typography gutterBottom variant="h6">
                                    认证-备注：{item.authenticationComment}
                                </Typography>
                            </Grid>) : <></>
                        }

                    </Grid>
                )}
                columns={[
                    {
                        label: "账号",
                        key: "userName"
                    }, {
                        label: "密码",
                        key: "password"
                    }, {
                        label: "管理员",
                        render: (item) => (
                            item.isAdmin ? (<p>是</p>) : (<p>否</p>)
                        )
                    }
                    , {
                        label: '余额',
                        key: 'balance'
                    }, {
                        label: '已充值',
                        key: 'actualPayAmount'
                    }
                    , {
                        label: '是否认证',
                        render: (item) => (
                            !!(item['authenticationThirdPartyId']) ? (<p>是</p>) : (<p>否</p>)
                        ),
                    }, {
                        label: '认证类型',
                        key: "authenticationType"
                    },
                    {
                        label: "操作",
                        render: (item) => (
                            <>
                                <Button
                                    startIcon={<DirectionsRailway style={{fontSize: 16}}/>}
                                    size="small"
                                    color="primary"
                                    className={classes.tableButton}
                                    onClick={() => travelToUser(item)}
                                    variant="contained">登录</Button>
                                <Button
                                    startIcon={<PermIdentity style={{fontSize: 16}}/>}
                                    size="small"
                                    color="primary"
                                    className={classes.tableButton}
                                    onClick={() => {
                                        setPermOpAccount(item);
                                        setShowPermOpDialog(true);
                                    }}
                                    variant="contained">配置权限</Button>
                                <Button
                                    startIcon={<SupervisorAccount style={{fontSize: 16}}/>}
                                    size="small"
                                    color="primary"
                                    className={classes.tableButton}
                                    onClick={() => grantAdmin(item)}
                                    variant="contained">{item.isAdmin ? "移除管理员" : "升级管理员"}</Button>
                                <Button
                                    startIcon={<AttachMoney style={{fontSize: 16}}/>}
                                    size="small"
                                    color="primary"
                                    className={classes.tableButton}
                                    onClick={() => {
                                        setChargeTargetUser(item);
                                        setOpenRechargeDialog(true)
                                    }}
                                    variant="contained">充值</Button>

                                <Button
                                    startIcon={<AssignmentInd style={{fontSize: 16}}/>}
                                    size="small"
                                    color="primary"
                                    className={classes.tableButton}
                                    onClick={() => {
                                        setAuthTargetUser(item);
                                        setOpenAuthDialog(true)
                                    }}
                                    disabled={!!(item['authenticationThirdPartyId'])}
                                    variant="contained">认证</Button>
                            </>
                        )
                    }
                ]}
            />

            <CreateUserDialog
                openCreateUserDialog={openCreateUserDialog}
                setOpenCreateUserDialog={setOpenCreateUserDialog}
                setRefresh={setRefresh}
            />

            <RechargeDialog
                openRechargeDialog={openRechargeDialog}
                setOpenRechargeDialog={setOpenRechargeDialog}
                setRefresh={setRefresh}
                chargeTargetUser={chargeTargetUser}
            />
            <AuthenticationDialog
                openAuthDialog={openAuthDialog}
                setOpenAuthDialog={setOpenAuthDialog}
                callback={() => {
                    setRefresh(+new Date())
                }}
                authTargetUser={authTargetUser}
                title={"管理员手动认证"}
                authFunction={api.adminCertification}
            />

            <OpeDialog title={"编辑权限:" + permOpAccount.userName} okText={"确认"} openDialog={showPermOpDialog}
                       fullScreen
                       setOpenDialog={setShowPermOpDialog}
                       opeContent={
                           (<Permission account={permOpAccount}
                                        setRefresh={setRefresh}
                           />)
                       }
            />
        </div>
    )
}

export default AccountList;