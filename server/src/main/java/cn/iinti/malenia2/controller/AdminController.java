package cn.iinti.malenia2.controller;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.entity.*;
import cn.iinti.malenia2.mapper.*;
import cn.iinti.malenia2.service.backend.CurrentBandWidthService;
import cn.iinti.malenia2.service.base.BroadcastService;
import cn.iinti.malenia2.service.base.UserInfoService;
import cn.iinti.malenia2.service.base.alert.EventNotifierService;
import cn.iinti.malenia2.service.base.config.ConfigService;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.config.SettingsValidate;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.proxy.dbconfigs.DbConfigs;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperUser;
import cn.iinti.malenia2.system.LoginRequired;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(BuildConfig.restfulApiPrefix + "/admin-op")
public class AdminController {
    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private ConfigService configService;

    @Resource
    private ServerNodeMapper serverNodeMapper;

    @Resource
    private SysLogMapper sysLogMapper;


    @Resource
    private UserOrderCurrentBandwidthMapper userOrderCurrentBandwidthMapper;

    @Resource
    private EventNotifierService eventNotifierService;

    @Resource
    private CurrentBandWidthService currentBandWidthService;

    @Resource
    private UserOrderMapper userOrderMapper;

    @Operation(summary = "创建用户")
    @LoginRequired(forAdmin = true, alert = true)
    @GetMapping("/createUser")
    public CommonRes<UserInfo> createUser(String userName, String password) {
        return userInfoService.register(userName, password);
    }

    @Operation(summary = "将一个用户升级为管理员")
    @LoginRequired(forAdmin = true, alert = true)
    @GetMapping("/grantAdmin")
    public CommonRes<String> grantAdmin(String userName, boolean isAdmin) {
        if (StringUtils.isBlank(userName)) {
            return CommonRes.failed("没有用户名");
        }
        if (isAdmin && Environment.isDemoSite) {
            return CommonRes.failed("测试demo网站不允许设置新的管理员");
        }
        return userInfoService.grantAdmin(userName, isAdmin);
    }

    @Operation(summary = "系统设置项配置模版")
    @GetMapping("/settingTemplate")
    @LoginRequired(forAdmin = true)
    public CommonRes<?> settingTemplate() {
        return CommonRes.success(Settings.allSettingsVo());
    }

    @Operation(summary = "所有的系统配置")
    @GetMapping("/allConfig")
    @LoginRequired(forAdmin = true)
    public CommonRes<List<SysConfig>> allConfig() {
        return configService.allConfig();
    }

    @Operation(summary = "修改系统配置,批量")
    @PostMapping("/setConfigs")
    @LoginRequired(forAdmin = true, alert = true)
    public CommonRes<String> setConfigs(@RequestBody Map<String, String> configs) {
        if (Environment.isDemoSite) {
            return CommonRes.failed("测试demo网站不允许修改配置");
        }
        String errorMsg = null;
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String msg = SettingsValidate.doValidate(entry.getKey(), entry.getValue());
            if (msg != null) {
                errorMsg = "error config: " + entry.getKey() + " " + msg;
                break;
            }
        }
        if (StringUtils.isNotBlank(errorMsg)) {
            return CommonRes.failed(errorMsg);
        }

        if (configs.isEmpty()) {
            return CommonRes.failed("no config passed");
        }

        for (Map.Entry<String, String> entry : configs.entrySet()) {
            CommonRes<SysConfig> res = configService.setConfig(entry.getKey(), entry.getValue());
            if (!res.isOk()) {
                return res.errorTransfer();
            }
        }
        return CommonRes.success("ok");

    }


    @Operation(summary = "修改系统配置")
    @PostMapping("/setConfig")
    @LoginRequired(forAdmin = true, alert = true)
    public CommonRes<SysConfig> setConfig(@RequestBody Map<String, String> data) {
        if (Environment.isDemoSite) {
            return CommonRes.failed("测试demo网站不允许修改配置");
        }
        String key = data.get("key");
        String value = data.get("value");
        String msg = SettingsValidate.doValidate(key, value);
        if (StringUtils.isNotBlank(msg)) {
            return CommonRes.failed(msg);
        }
        CommonRes<SysConfig> ret = configService.setConfig(key, value);
        BroadcastService.triggerEvent(BroadcastService.Topic.CONFIG);
        return ret;
    }


    @Operation(summary = "管理员穿越到普通用户，获取普通用户token")
    @LoginRequired(forAdmin = true)
    @GetMapping("/travelToUser")
    public CommonRes<UserInfo> travelToUser(Long id) {
        UserInfo toUser = userInfoMapper.selectById(id);
        if (toUser == null) {
            return CommonRes.failed("user not exist");
        }
        toUser.setLoginToken(userInfoService.genLoginToken(toUser, LocalDateTime.now()));
        return CommonRes.success(toUser);
    }

    @Operation(summary = "用户列表")
    @LoginRequired(forAdmin = true)
    @GetMapping("/listUser")
    public CommonRes<IPage<UserInfo>> listUser(int page, int pageSize) {
        if (page < 1) {
            page = 1;
        }
        return CommonRes.success(userInfoMapper.selectPage(new Page<>(page, pageSize), new QueryWrapper<>()));

    }

    @Operation(summary = "列出 server")
    @LoginRequired(forAdmin = true)
    @GetMapping("/listServer")
    public CommonRes<IPage<ServerNode>> listServer(int page, int pageSize) {
        if (page < 1) {
            page = 1;
        }
        QueryWrapper<ServerNode> queryWrapper = new QueryWrapper<ServerNode>()
                .orderByDesc(ServerNode.LAST_ACTIVE_TIME);
        return CommonRes.success(serverNodeMapper.selectPage(new Page<>(page, pageSize), queryWrapper));
    }


    @Operation(summary = "设置服务器启用状态")
    @GetMapping("setServerStatus")
    @LoginRequired(forAdmin = true, alert = true)
    public CommonRes<ServerNode> setServerStatus(Long id, Boolean enable) {
        return CommonRes.ofPresent(serverNodeMapper.selectById(id))
                .ifOk(serverNode -> {
                    serverNode.setEnable(enable);
                    serverNodeMapper.updateById(serverNode);
                });
    }

    @Operation(summary = "查询操作日志")
    @GetMapping("/listSystemLog")
    @LoginRequired(forAdmin = true)
    public CommonRes<IPage<SysLog>> listSystemLog(String username, String operation, int page, int pageSize) {
        QueryWrapper<SysLog> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.eq(SysLog.USERNAME, username);
        }
        if (StringUtils.isNotBlank(operation)) {
            queryWrapper.eq(SysLog.OPERATION, operation);
        }
        queryWrapper.orderByDesc(SysLog.ID);
        return CommonRes.success(sysLogMapper.selectPage(new Page<>(page, pageSize), queryWrapper));
    }

    ////////////////////////////////////// malenia custom //////////////////////////////////////

    @Operation(summary = "(管理员专用)用户充值")
    @LoginRequired(forAdmin = true, alert = true)
    @GetMapping("/rechargeUser")
    public CommonRes<Double> recharge(String user, double amount, double actualPayAmount, String rechargeComment) {
        return CommonRes.success(userInfoService.recharge(user, amount, actualPayAmount, rechargeComment, null, null));
    }

    @Operation(summary = "调整带宽限制，同时修改价格")
    @GetMapping("/setOrderBandwidthLimit")
    @LoginRequired(forAdmin = true, alert = true)
    public CommonRes<Boolean> setOrderBandwidthLimit(long orderId, double bandwidthLimit, double newOrderPrice) {
        return userInfoService.setOrderBandwidthLimit(orderId, bandwidthLimit, newOrderPrice);
    }

    @Operation(summary = "触发生产环境带宽重新设置,将会立即应用订单最新的带宽配额限制，给管理员运维使用（请注意本接口针对被调用的单台机器生效，多节点需要分别调用刷新）")
    @GetMapping("/triggerReloadBandwidthLimit")
    @LoginRequired(forAdmin = true, alert = true, apiToken = true)
    public CommonRes<String> triggerReloadBandwidthLimit() {
        currentBandWidthService.scheduleRefreshOrderBandwidth();
        return CommonRes.success("ok");
    }


    @LoginRequired(forAdmin = true)
    @GetMapping("/findUserByIp")
    public CommonRes<UserInfo> findUserByIp(String ip) {
        return CommonRes.ofPresent(DbConfigs.testCidrIp(ip))
                .transform(WrapperUser::getUserInfo);
    }

    @Operation(summary = "(管理员专用)当前带宽使用最高的5个订单，在提供ip资源售卖能力的时候，方便监控最高用量产品，好产品策略调整")
    @LoginRequired(forAdmin = true)
    @GetMapping("/top10BandwidthOrder")
    public CommonRes<List<UserOrder>> top10BandwidthOrder() {
        Map<Long, Double> orderBandwidth = Maps.newHashMap();
        userOrderCurrentBandwidthMapper.selectList(new QueryWrapper<>())
                .forEach(userOrderCurrentBandwidth -> {
                    // 对于每个订单，在多台服务器上都会产生带宽，所以这里要聚合
                    Long userOrderId = userOrderCurrentBandwidth.getUserOrderId();
                    double bandwidth = orderBandwidth.getOrDefault(userOrderId, 0.0)
                            + userOrderCurrentBandwidth.getCurrentBandwidth();
                    orderBandwidth.put(userOrderId, bandwidth);
                });

        List<Long> topOrder = orderBandwidth.entrySet()
                .stream()
                .sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topOrder.isEmpty()) {
            return CommonRes.success(Collections.emptyList());
        }
        List<UserOrder> userOrders = userOrderMapper.selectList(new QueryWrapper<UserOrder>().in(UserOrder.ID, topOrder));
        userOrders.forEach(userOrder -> {
            Double bandwidth = orderBandwidth.get(userOrder.getId());
            if (bandwidth != null) {
                userOrder.setRuntimeBandwidth(bandwidth * 8 / (1024 * 1024));
            }
        });
        userOrders.sort((o1, o2) -> Double.compare(o2.getRuntimeBandwidth(), o1.getRuntimeBandwidth()));
        return CommonRes.success(userOrders);
    }

    @LoginRequired(forAdmin = true)
    @Operation(summary = "管理员手动添加用户实名认证")
    @PostMapping("/adminCertification")
    public CommonRes<String> adminCertification(@RequestBody UserInfo userInfo) {
        return userInfoService.doAuthentication(userInfo, true);
    }
}
