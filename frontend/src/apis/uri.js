import config from "config";

const api_prefix = config.api_prefix;

export default {
    // system
    systemInfo: api_prefix + "/system/systemInfo", // 系统信息，后台系统配置，页面加载加载一次
    notice: api_prefix + "/system/systemNotice",//系统通告信息
    getIntPushMsg: "/yint-stub/certificate/getIntPushMsg", // 授权信息(仅限因体加密环境)
    getNowCertificate: "/yint-stub/certificate/getNowCertificate", // 授权证书(仅限因体加密环境)


    // admin
    travelToUser: api_prefix + "/admin-op/travelToUser", // 模拟登录
    userAdd: api_prefix + "/admin-op/createUser", // 创建用户
    userList: api_prefix + "/admin-op/listUser", // 用户列表
    setConfig: api_prefix + "/admin-op/setConfig post", // config 单条
    setConfigs: api_prefix + "/admin-op/setConfigs post", // config all
    allConfig: api_prefix + "/admin-op/allConfig", // 所有 config
    settingTemplate: api_prefix + "/admin-op/settingTemplate",// 设置配置模版
    listServer: api_prefix + "/admin-op/listServer", // 列出 server
    setServerStatus: api_prefix + "/admin-op/setServerStatus", // 设置服务器状态
    grantAdmin: api_prefix + "/admin-op/grantAdmin", // 授权
    logList: api_prefix + "/admin-op/listSystemLog", // 日志


    // user
    login: api_prefix + "/user-info/login post query", // 登录
    register: api_prefix + "/user-info/register post query", // 注册
    getUser: api_prefix + "/user-info/userInfo", // 获取用户信息
    updatePassword: api_prefix + "/user-info/resetPassword post query", // 修改密码
    refreshToken: api_prefix + "/user-info/refreshToken", // 刷新 token
    regenerateAPIToken: api_prefix + "/user-info/regenerateAPIToken", // 刷新 api token
    permScopes: api_prefix + "/user-info/permScopes",
    permItemsOfScope: api_prefix + "/user-info/permItemsOfScope",
    editUserPerm: api_prefix + "/user-info/editUserPerm post query",


    // metric
    queryMetric: api_prefix + "/metric/queryMetric",// 查询指标
    metricNames: api_prefix + "/metric/metricNames",// 指标列表
    metricTag: api_prefix + "/metric/metricTag",// 指标tag分量
    deleteMetric: api_prefix + "/metric/deleteMetric",// 删除一个指标
    mqlQuery: api_prefix + "/metric/mqlQuery post query",// 使用mql查询指标数据，mql支持指标内容的加工
    allMetricConfig: api_prefix + "/metric/allMetricConfig",


    // 下列为各业务自定义接口
    ///////////////////////////////////////// 下列为各业务自定义接口 ////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 管理员对用户的操作
    rechargeUser: api_prefix + "/admin-op/rechargeUser get", // (管理员专用)用户充值
    setOrderBandwidthLimit: api_prefix + "/admin-op/setOrderBandwidthLimit get", // 调整带宽限制，同时修改价格
    findUserByIp: api_prefix + "/admin-op/findUserByIp get", // 根据出口ip查询配置该白名单的用户
    top10BandwidthOrder: api_prefix + "/admin-op/top10BandwidthOrder", // 当前带宽使用最高的5个订单
    adminCertification: api_prefix + "/admin-op/adminCertification post", // 管理员手动添加用户实名认证

    // 管理员后台代理相关资源配置操作
    editProduct: api_prefix + "/admin-config/editProduct post", // 新增/更新代理产品
    getProductDetail: api_prefix + "/admin-config/getProductDetail", // 获取单个产品详情
    updateProductStatus: api_prefix + "/admin-config/updateProductStatus", // 更改代理产品生效状态
    productPorts: api_prefix + "/admin-config/productPorts", // 当前产品启动的代理服务端口列表
    listProductIpSources: api_prefix + "/admin-config/listProductIpSources", //  获取产品下的代理Ip资源
    addSourceToProduct: api_prefix + "/admin-config/addSourceToProduct", // 添加代理Ip资源到产品
    deleteIpSourceFromProduct: api_prefix + "/admin-config/deleteIpSourceFromProduct", // 移除代理上游资源

    getAllIpSources: api_prefix + "/admin-config/getAllIpSources", // 全部代理Ip资源
    getIpSourceDetail: api_prefix + "/admin-config/getIpSourceDetail", // 获取单个IP资源详情
    editIpSource: api_prefix + "/admin-config/editIpSource post", // 新增/更新代理上游资源
    getResourceContentList: api_prefix + "/admin-config/getResourceContentList", // 获取ip资源池IP列表
    lastErrorRecords4IpSource: api_prefix + "/admin-config/lastErrorRecords4IpSource", // 获取ip资源池报错记录

    listRechargeRecords: api_prefix + "/admin-config/listRechargeRecords", // 充值记录列表
    exportRechargeRecord: api_prefix + "/admin-config/exportRechargeRecord", // 导出充值记录

    // 普通用户使用资源的相关接口
    addWhiteIp: api_prefix + "/user-op/addWhiteIp", // 添加白名单
    listAuthWhiteIp: api_prefix + "/user-op/listAuthWhiteIp", // 列出当前账户的白名单出口ip
    deleteAuthWhiteIp: api_prefix + "/user-op/deleteAuthWhiteIp", // 删除某个出口ip配置
    setupAuthAccount: api_prefix + "/user-op/setupAuthAccount", // 设置当前用户的鉴权账户数据
    purchase: api_prefix + "/user-op/purchase", // 购买代理产品接口
    updateOrder: api_prefix + "/user-op/updateOrder", // 编辑订单
    editMitmScript: api_prefix + "/user-op/editMitmScript post", // 修改或者新增MITM脚本
    listMitmScript: api_prefix + "/user-op/listMitmScript", // 展示当前用户的所有mitm脚本，不需要分页，因为一个用户的配置正常应该是不多于10个的
    removeMitmScript: api_prefix + "/user-op/removeMitmScript", // 删除mitm脚本
    uploadAsset: api_prefix + "/user-op/uploadAsset form", // 上传一个资产文件
    listAsset: api_prefix + "/user-op/listAsset", // 查看当前用户拥有的资产文件
    deleteAsset: api_prefix + "/user-op/deleteAsset", // 删除一个资产文件
    downloadAsset: api_prefix + "/user-op/downloadAsset", // 下载资产文件
    listAccessRecord: api_prefix + "/user-op/listAccessRecord", // 当前账户使用代理资源的日志，包含访问那些网站和对应网站的频率。系统存储1个月内的所有记录
    listRateBill: api_prefix + "/user-op/listRateBill", // 查询带宽使用流水
    listBill: api_prefix + "/user-op/listBill", // 查询订单流水
    exportBill: api_prefix + "/user-op/exportBill", // 导出订单流水
    listAllProducts: api_prefix + "/user-op/listAllProducts", // 全量代理产品列表
    listOrder: api_prefix + "/user-op/listOrder", // 列出已购买的产品订单列表
    orderDetail: api_prefix + "/user-op/orderDetail", // 列出已购买的产品订单列表
    downloadCaCertificate: api_prefix + "/user-op/downloadCaCertificate", // 下载ca证书
    certification: api_prefix + "/user-op/certification post",// 实名认证

    // 充值相关
    rechargeRecord: api_prefix + "/pay/rechargeRecord",
    prepare_alipay: api_prefix + "/pay/prepare_alipay",
    prepare_wechat: api_prefix + "/pay/prepare_wechat",
};
