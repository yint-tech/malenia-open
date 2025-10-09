package cn.iinti.malenia2.controller;


import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.api.mitm.interceptor.GroovyMitmScript;
import cn.iinti.malenia2.entity.*;
import cn.iinti.malenia2.mapper.*;
import cn.iinti.malenia2.service.base.BroadcastService;
import cn.iinti.malenia2.service.base.UserInfoService;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.base.perm.PermsService;
import cn.iinti.malenia2.service.base.storage.StorageManager;
import cn.iinti.malenia2.service.proxy.auth.IpTrie;
import cn.iinti.malenia2.service.proxy.core.mitm.api.ApiImpl;
import cn.iinti.malenia2.service.proxy.core.ssl.ServerSSLContextManager;
import cn.iinti.malenia2.service.proxy.dbconfigs.DbConfigs;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperUser;
import cn.iinti.malenia2.system.AppContext;
import cn.iinti.malenia2.system.LoginRequired;
import cn.iinti.malenia2.utils.CommonUtils;
import cn.iinti.malenia2.utils.ExcelUtils;
import cn.iinti.malenia2.utils.ServletUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 普通用户的操作接口，作为普通产品使用者
 * </p>
 *
 * @author virjar
 * @since 2022-02-22
 */
@RestController
@Slf4j
@RequestMapping(BuildConfig.restfulApiPrefix + "/user-op")
public class UserOperatorController {

    @Resource
    private AuthWhiteIpMapper authWhiteIpMapper;


    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private UserOrderMapper userOrderMapper;

    @Resource
    private MitmScriptMapper mitmScriptMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private AssetMapper assetMapper;


    @Resource
    private AccessRecordMapper accessRecordMapper;

    @Resource
    private OrderRateBillMapper orderRateBillMapper;

    @Resource
    private BillMapper billMapper;

    @Resource
    private PermsService permsService;

    @Resource
    private UserInfoService userInfoService;


    @LoginRequired(apiToken = true)
    @Operation(summary = "添加白名单")
    @GetMapping("/addWhiteIp")
    public CommonRes<String> addWhiteIp(HttpServletRequest httpServletRequest) {
        String ip = httpServletRequest.getParameter("ip");
        if (StringUtils.isBlank(ip)) {
            ip = httpServletRequest.getHeader("X-Real-IP");
            if (StringUtils.isBlank(ip)) {
                ip = httpServletRequest.getRemoteAddr();
                if ("127.0.0.1".equals(ip)) {
                    return CommonRes.failed("系统错误");
                }
            }
        }
        String comment = httpServletRequest.getParameter("comment");
        UserInfo user = AppContext.getUser();
        List<AuthWhiteIp> authWhiteIps = authWhiteIpMapper.selectList(new QueryWrapper<AuthWhiteIp>()
                .eq(AuthWhiteIp.BIND_USER, user.getUserName()));


        IpTrie<AuthWhiteIp> ipIpTrie = new IpTrie<>();
        for (AuthWhiteIp authWhiteIp : authWhiteIps) {
            if (authWhiteIp.getWhiteIp().equals(ip)) {
                //已经存在
                return CommonRes.success("already exist:" + ip);
            }
            ipIpTrie.insert(authWhiteIp.getWhiteIp(), authWhiteIp);
        }
        AuthWhiteIp cidrAuthWhiteIp = ipIpTrie.find(ip);
        if (cidrAuthWhiteIp != null) {
            return CommonRes.success("already exist:" + cidrAuthWhiteIp.getWhiteIp());
        }


        if (authWhiteIps.size() > Settings.maxWhiteIpPerUser.value) {
            //超过了配置限度，删除来自接口API增加的白名单的最早的一个
            Optional<AuthWhiteIp> firstAPIConfig = authWhiteIps.stream()
                    .filter(AuthWhiteIp::getFromApi)
                    .min(Comparator.comparing(AuthWhiteIp::getCreateTime));
            if (firstAPIConfig.isPresent()) {
                authWhiteIpMapper.deleteById(firstAPIConfig.get().getId());
                log.info("remove oldest because of config quota limited :{}", firstAPIConfig.get().getWhiteIp());
            } else {
                return CommonRes.failed("max white ip configItems quota limited");
            }
        }


        WrapperUser wrapperUser = DbConfigs.testCidrIp(ip);
        if (wrapperUser != null) {
            if (!StringUtils.equals(wrapperUser.getUserInfo().getUserName(), user.getUserName())) {
                return CommonRes.failed("duplicate AIDR space config with user: " + wrapperUser.getUserInfo().getUserName());
            }
        }

        AuthWhiteIp authWhiteIp = new AuthWhiteIp();
        authWhiteIp.setBindUser(user.getUserName());
        authWhiteIp.setWhiteIp(ip);
        authWhiteIp.setComment(comment);
        authWhiteIp.setFromApi(AppContext.isApiUser());

        authWhiteIpMapper.insert(authWhiteIp);
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return CommonRes.success(ip);
    }

    @LoginRequired
    @Operation(summary = "列出当前账户的白名单出口ip")
    @GetMapping("/listAuthWhiteIp")
    public CommonRes<List<AuthWhiteIp>> listAuthWhiteIp() {
        UserInfo user = AppContext.getUser();
        return CommonRes.success(authWhiteIpMapper.selectList(new QueryWrapper<AuthWhiteIp>()
                .eq(AuthWhiteIp.BIND_USER, user.getUserName())));
    }


    @LoginRequired(apiToken = true)
    @Operation(summary = "删除某个出口ip配置")
    @GetMapping("/deleteAuthWhiteIp")
    public CommonRes<String> deleteAuthWhiteIp(Long id) {
        UserInfo user = AppContext.getUser();
        AuthWhiteIp authWhiteIp = authWhiteIpMapper.selectById(id);
        if (authWhiteIp == null) {
            return CommonRes.failed("record not found");
        }
        if (!authWhiteIp.getBindUser().equals(user.getUserName())
                && !user.getIsAdmin()
        ) {
            return CommonRes.failed("permission deny");
        }

        authWhiteIpMapper.deleteById(id);
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return CommonRes.success("ok");
    }


    @LoginRequired
    @Operation(summary = "设置当前用户的鉴权账户数据(由于需要刷新前端视图，所以这里返回前端实体)")
    @GetMapping("/setupAuthAccount")
    public CommonRes<UserInfo> setupAuthAccount(@NotBlank String authAccount, @NotBlank String authPassword) {
        Set<String> illegalWords = Sets.newHashSet("-", "@");
        for (String illegalWord : illegalWords) {
            if (StringUtils.contains(authAccount, illegalWord) || StringUtils.contains(authPassword, illegalWord)) {
                return CommonRes.failed("鉴权账户/密码不能包含非法字符： " + illegalWord);
            }
        }
        UserInfo one = userInfoMapper.selectOne(new QueryWrapper<UserInfo>().eq(UserInfo.AUTH_ACCOUNT, authAccount));
        UserInfo mUser = AppContext.getUser();
        if (one != null && !one.getId().equals(mUser.getId())) {
            return CommonRes.failed("the authAccount: " + authAccount + " already exist");
        }

        one = userInfoMapper.selectOne(new QueryWrapper<UserInfo>().eq(UserInfo.USER_NAME, authAccount));
        if (one != null && !one.getId().equals(mUser.getId())) {
            return CommonRes.failed("the authAccount: " + authAccount + " already exist");
        }

        userInfoMapper.update(null, new UpdateWrapper<UserInfo>()
                .eq(UserInfo.ID, mUser.getId())
                .set(UserInfo.AUTH_ACCOUNT, authAccount)
                .set(UserInfo.AUTH_PWD, authPassword)
        );

        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return CommonRes.success(userInfoMapper.selectById(mUser.getId()));
    }


    @LoginRequired
    @Operation(summary = "实名认证")
    @PostMapping("/certification")
    public CommonRes<String> certification(@RequestBody UserInfo userInfo) {
        if (!Environment.supportCertification) {
            return CommonRes.failed("未开启实名认证功能!请联系系统管理员");
        }
        return userInfoService.doAuthentication(userInfo, false);
    }

    @Operation(summary = "购买代理产品接口")
    @LoginRequired
    @GetMapping("/purchase")
    public CommonRes<String> purchase(String productId, UserOrder.EnumBalanceMethod balanceMethod,
                                      String referer) {
        UserInfo user = AppContext.getUser();
        if (user.getBalance() <= 0) {
            return CommonRes.failed("no balance left");
        }
        String username = user.getUserName();
        Product product = productMapper.selectOne(new QueryWrapper<Product>().eq(Product.PRODUCT_ID, productId));
        if (product == null) {
            return CommonRes.failed("产品ID不存在:" + productId);
        }

        UserOrder one = userOrderMapper.selectOne(new QueryWrapper<UserOrder>().eq(UserOrder.PRODUCT_ID, productId)
                .eq(UserOrder.PURCHASE_USER, username));

        double originPrice = product.getFlowPrice();
        if (balanceMethod == UserOrder.EnumBalanceMethod.METHOD_HOUR) {
            originPrice = product.getHourPrice();
        }
        if (one == null) {
            one = new UserOrder();
            one.setProductId(productId);
            one.setProductName(product.getProductName());
            one.setPurchaseUser(username);
            one.setBalancePrice(originPrice);
            one.setBalanceMethod(balanceMethod);
            one.setReferer(referer);
            one.setBandwidthLimit(Settings.defaultBandwidthLimit.value.doubleValue());
            userOrderMapper.insert(one);
        } else {
            userOrderMapper.updateById(one);
        }
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return CommonRes.success("ok");
    }


    @Operation(summary = "编辑订单")
    @LoginRequired
    @GetMapping("/updateOrder")
    public CommonRes<String> updateRandomTuningStatus(
            @NotBlank String productId, boolean enabled, boolean randomTurning,
            @NotNull UserOrder.EnumBalanceMethod balanceMethod, int connectTimeout, int maxFailoverCount) {
        if (connectTimeout < 2000) {
            return CommonRes.failed("连接超时时间至少应该为：2000");
        }
        if (maxFailoverCount < 1) {
            return CommonRes.failed("failover次数至少应为：1");
        }
        String userName = AppContext.getUser().getUserName();
        UserOrder userOrder = userOrderMapper.selectOne(
                new QueryWrapper<UserOrder>().eq(UserOrder.PRODUCT_ID, productId)
                        .eq(UserOrder.PURCHASE_USER, userName));
        if (userOrder == null) {
            return CommonRes.failed("当前订单不存在");
        }
        userOrder.setEnabled(enabled);
        userOrder.setRandomTurning(randomTurning);
        userOrder.setConnectTimeout(connectTimeout);
        userOrder.setMaxFailoverCount(maxFailoverCount);

        if (userOrder.getBalanceMethod() != balanceMethod) {
            Double bandwidthLimit = userOrder.getBandwidthLimit();
            double price;
            // 重新计算价格
            Product product = productMapper.selectOne(new QueryWrapper<Product>().eq(Product.PRODUCT_ID, productId));
            if (balanceMethod == UserOrder.EnumBalanceMethod.METHOD_FLOW) {
                price = product.getFlowPrice();
            } else {
                //按照带宽付费，需要看带宽的配置
                if (bandwidthLimit == null || userOrder.getBandwidthBalancePrice() == null) {
                    price = product.getHourPrice();
                    bandwidthLimit = Settings.defaultBandwidthLimit.value.doubleValue();
                    userOrder.setBandwidthBalancePrice(price);
                } else {
                    price = userOrder.getBandwidthBalancePrice();
                }

            }
            userOrder.setBalancePrice(price);
            userOrder.setBalanceMethod(balanceMethod);
            userOrder.setBandwidthLimit(bandwidthLimit);
        }
        userOrderMapper.updateById(userOrder);
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return CommonRes.success("ok");
    }

    @Operation(summary = "修改或者新增MITM脚本")
    @LoginRequired
    @PostMapping("/editMitmScript")
    public CommonRes<MitmScript> editMitmScript(@RequestBody MitmScript mitmScript) {
        GroovyMitmScript.compileScript(mitmScript.getContent(), new ApiImpl(mitmScript.getUser()));

        UserInfo user = AppContext.getUser();
        mitmScript.setUser(user.getUserName());

        MitmScript one = mitmScriptMapper.selectOne(new QueryWrapper<MitmScript>()
                .eq(MitmScript.USER, mitmScript.getUser())
                .eq(MitmScript.NAME, mitmScript.getName())
        );
        if (one != null) {
            mitmScript.setId(one.getId());
            mitmScript.setUpdateTime(LocalDateTime.now());
            mitmScriptMapper.updateById(mitmScript);
        } else {
            mitmScript.setUpdateTime(LocalDateTime.now());
            mitmScriptMapper.insert(mitmScript);
        }
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return CommonRes.success(mitmScript);
    }

    @Operation(summary = "展示当前用户的所有mitm脚本，不需要分页，因为一个用户的配置正常应该是不多于10个的")
    @LoginRequired
    @GetMapping("/listMitmScript")
    public CommonRes<List<MitmScript>> listMitmScript() {
        return CommonRes.success(mitmScriptMapper.selectList(new QueryWrapper<MitmScript>().eq(MitmScript.USER, AppContext.getUser().getUserName())));
    }

    @Operation(summary = "删除mitm脚本")
    @LoginRequired
    @GetMapping("/removeMitmScript")
    public CommonRes<String> removeMitmScript(Long id) {
        UserInfo user = AppContext.getUser();

        MitmScript one = mitmScriptMapper.selectOne(new QueryWrapper<MitmScript>()
                .eq(MitmScript.ID, id)
        );
        if (one == null) {
            return CommonRes.failed("record not exist!!");
        }
        if (!user.getUserName().equals(one.getUser())) {
            return CommonRes.failed("resource operation deny");
        }
        mitmScriptMapper.deleteById(id);
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return CommonRes.success("ok");
    }


    @Operation(summary = "上传一个资产文件")
    @LoginRequired
    @PostMapping("/uploadAsset")
    public CommonRes<String> uploadAsset(String path, MultipartFile file) {
        UserInfo user = AppContext.getUser();
        return ServletUtil.uploadToTempNoCheck(file, file1 -> {
            Asset asset = assetMapper.selectOne(new QueryWrapper<Asset>()
                    .eq(Asset.USER, user.getUserName())
                    .eq(Asset.PATH, path)
            );
            if (asset != null) {
                StorageManager.deleteFile(asset.buildStoragePath());
            } else {
                asset = new Asset();
                asset.setUser(user.getUserName());
                asset.setPath(path);
                asset.setFileSize(file1.length());
            }
            StorageManager.store(asset.buildStoragePath(), file1);
            asset.setUpdateTime(LocalDateTime.now());
            if (asset.getId() == null) {
                assetMapper.insert(asset);
            } else {
                assetMapper.updateById(asset);
            }
            return CommonRes.success("ok");
        });

    }

    @Operation(summary = "查看当前用户拥有的资产文件")
    @LoginRequired
    @GetMapping("/listAsset")
    public CommonRes<List<Asset>> listAsset() {
        return CommonRes.success(assetMapper.selectList(new QueryWrapper<Asset>().eq(Asset.USER,
                AppContext.getUser().getUserName()))
        );
    }

    @Operation(summary = "删除一个资产文件")
    @LoginRequired
    @GetMapping("/deleteAsset")
    public CommonRes<String> deleteAsset(Long id) {
        return CommonRes.ofPresent(assetMapper.selectOne(new QueryWrapper<Asset>().eq(Asset.ID, id)))
                .acceptIfOk(res -> {
                    Asset asset = res.getData();
                    if (!asset.getUser().equals(AppContext.getUser().getUserName())) {
                        res.changeFailed("record not exist");
                        return;
                    }
                    StorageManager.deleteFile(asset.buildStoragePath());
                    assetMapper.deleteById(asset.getId());
                }).transform(asset -> "ok");


    }

    @Operation(summary = "下载资产文件")
    @LoginRequired
    @GetMapping("/downloadAsset")
    public void downloadAsset(Long id) {
        Asset asset = assetMapper.selectOne(new QueryWrapper<Asset>().eq(Asset.ID, id));
        if (asset == null || !AppContext.getUser().getUserName().equals(asset.getUser())) {
            ServletUtil.writeRes(CommonRes.failed("record not found"));
            return;
        }
        ServletUtil.responseFile(StorageManager.get(asset.buildStoragePath()),
                "application/octet-stream");
    }


    @Operation(summary = "当前账户使用代理资源的日志，包含访问那些网站和对应网站的频率。系统存储1个月内的所有记录")
    @LoginRequired(apiToken = true)
    @GetMapping("/listAccessRecord")
    public CommonRes<IPage<AccessRecord>> listAccessRecord(int pageSize, int page) {
        QueryWrapper<AccessRecord> queryWrapper = new QueryWrapper<AccessRecord>()
                .eq(AccessRecord.ACCESS_USER, AppContext.getUser().getUserName());
        queryWrapper.orderByDesc(AccessRecord.ID);
        return CommonRes.success(accessRecordMapper.selectPage(new Page<>(page, pageSize), queryWrapper));
    }

    @Operation(summary = "查询带宽使用流水")
    @LoginRequired
    @GetMapping("/listRateBill")
    public CommonRes<IPage<OrderRateBill>> listRateBill(Long orderId, int pageSize, int page) {
        UserOrder userOrder = userOrderMapper.selectById(orderId);
        if (userOrder == null) {
            return CommonRes.failed("order not exist");
        }
        if (!StringUtils.equals(AppContext.getUser().getUserName(), userOrder.getPurchaseUser())) {
            return CommonRes.failed("order id error");
        }
        QueryWrapper<OrderRateBill> queryWrapper = new QueryWrapper<OrderRateBill>()
                .eq(OrderRateBill.ORDER_ID, orderId);

        queryWrapper.orderByDesc(OrderRateBill.ID);
        IPage<OrderRateBill> orderRateUsageIPage = orderRateBillMapper.selectPage(new Page<>(page, pageSize), queryWrapper);
        orderRateUsageIPage.getRecords().sort(Comparator.comparing(OrderRateBill::getTimeKey));
        return CommonRes.success(orderRateUsageIPage);
    }

    @Operation(summary = "查询订单流水")
    @LoginRequired
    @GetMapping("/listBill")
    public CommonRes<List<Bill>> listBill(String productId) {
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<Bill>()
                .eq(Bill.PURCHASE_USER, AppContext.getUser().getUserName());
        if (StringUtils.isNotBlank(productId)) {
            queryWrapper.eq(Bill.PRODUCT_ID, productId);
        }
        queryWrapper.orderByDesc(Bill.ID);
        return CommonRes.success(billMapper.selectList(queryWrapper));
    }

    private static final DateTimeFormatter exportDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Operation(summary = "导出订单流水")
    @LoginRequired
    @GetMapping("/exportBill")
    public void exportBill(@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                           HttpServletResponse response) {
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<Bill>()
                .eq(Bill.PURCHASE_USER, AppContext.getUser().getUserName())
                .gt(Bill.CREATE_TIME, startTime)
                .lt(Bill.CREATE_TIME, endTime);
        ExcelUtils.setExcelExportResponseHeader(response,
                "malenia-bill-" + AppContext.getUser().getUserName() +
                        startTime.format(exportDateTimeFormatter) + "-" +
                        endTime.format(exportDateTimeFormatter) +
                        ".xls");
        try (OutputStream os = response.getOutputStream()) {
            ExcelUtils.doExport(Bill.class, billMapper, queryWrapper, os);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    @Operation(summary = "全量代理产品列表")
    @LoginRequired
    @GetMapping("/listAllProducts")
    public CommonRes<List<Product>> getAllProducts() {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        UserInfo user = AppContext.getUser();
        if (!user.getIsAdmin()) {
            permsService.filter(Product.class, queryWrapper);
        }

        return CommonRes.success(productMapper.selectList(queryWrapper));
    }

    @Operation(summary = "获取单个产品详情")
    @LoginRequired
    @GetMapping("/getProductDetail")
    public CommonRes<Product> getProductDetail(@NotBlank String productId) {
        return CommonRes.ofPresent(productMapper.selectOne(
                new QueryWrapper<Product>().eq(Product.PRODUCT_ID, productId)
        )).acceptIfOk(res -> {
            UserInfo user = AppContext.getUser();
            if (user.getIsAdmin()) {
                return;
            }

            if (!permsService.hasPermission(Product.class, res.getData())) {
                res.changeFailed("permission deny");
            }
        });
    }

    /**
     * 列出已购买的产品订单
     */
    @Operation(summary = "列出已购买的产品订单列表")
    @LoginRequired
    @GetMapping("/listOrder")
    public CommonRes<List<UserOrder>> listOrder() {
        String username = AppContext.getUser().getUserName();
        List<UserOrder> userOrders = userOrderMapper
                .selectList(new QueryWrapper<UserOrder>()
                        .eq(UserOrder.PURCHASE_USER, username));
        List<Product> products = productMapper.selectList(new QueryWrapper<Product>().eq(Product.ENABLED, true));
        List<String> productIds = products.stream().map(Product::getProductId).collect(Collectors.toList());
        userOrders = userOrders.stream()
                .filter(x -> productIds.contains(x.getProductId()))
                .collect(Collectors.toList());
        return CommonRes.success(userOrders);
    }

    @Operation(summary = "某个订单详情")
    @LoginRequired
    @GetMapping("/orderDetail")
    public CommonRes<JSONObject> orderDetail(@NotBlank String productId) {
        String username = AppContext.getUser().getUserName();
        Product product = productMapper.selectOne(new QueryWrapper<Product>().eq(Product.PRODUCT_ID, productId));
        if (product == null) {
            return CommonRes.failed("product disabled");
        }

        UserOrder userOrder = userOrderMapper
                .selectOne(new QueryWrapper<UserOrder>()
                        .eq(UserOrder.PURCHASE_USER, username)
                        .eq(UserOrder.PRODUCT_ID, productId)
                );

        return CommonRes.ofPresent(userOrder).transform(userOrder1 -> {
            JSONObject jsonObject = (JSONObject) JSONObject.toJSON(userOrder1);
            jsonObject.putAll((JSONObject) JSON.toJSON(product));
            return jsonObject;
        });
    }


    @Operation(summary = "下载根证书，用户需要倒入根证书到自己电脑的证书库，并且信任他，否则无法开启https抓包")
    @GetMapping("/downloadCaCertificate")
    public ResponseEntity<byte[]> downloadCaCertificate(boolean pem) {
        HttpHeaders header = new HttpHeaders();
        header.add("Content-Disposition", "attachment;filename=Malenia2Proxy." + (pem ? "pem" : "crt"));
        header.add("content-type", pem ? "application/x-pem-file" : "application/x-x509-ca-cert");

        try {
            return new ResponseEntity<>(
                    ServerSSLContextManager.instance.getKeyStoreGenerator().exportRootCert(pem)
                    , header, HttpStatus.OK
            );
        } catch (CertificateEncodingException e) {
            log.error("error", e);
            return new ResponseEntity<>(CommonUtils.throwableToString(e).getBytes(StandardCharsets.UTF_8), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
