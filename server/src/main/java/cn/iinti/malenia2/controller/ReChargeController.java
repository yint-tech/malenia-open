package cn.iinti.malenia2.controller;


import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.entity.CommonRes;
import cn.iinti.malenia2.entity.RechargeRecord;
import cn.iinti.malenia2.entity.UserInfo;
import cn.iinti.malenia2.mapper.RechargeRecordMapper;
import cn.iinti.malenia2.mapper.UserInfoMapper;
import cn.iinti.malenia2.service.base.UserInfoService;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.system.AppContext;
import cn.iinti.malenia2.system.LoginRequired;
import cn.iinti.malenia2.utils.ServletUtil;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayConstants;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.base.Suppliers;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/**
 * <p>
 * 支付信息 前端控制器
 * </p>
 *
 * @author zepeng.li
 * @since 2024-01-30
 */
@RestController
@RequestMapping(BuildConfig.restfulApiPrefix + "/pay")
@Tag(name = "用户在线充值")
@Slf4j
public class ReChargeController {
    @Resource
    private RechargeRecordMapper rechargeRecordMapper;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserInfoMapper userInfoMapper;

    private static final String ALIPAY_GATE_WAY = "https://openapi.alipay.com/gateway.do";
    private static final Supplier<DefaultAlipayClient> alipayClient = Suppliers.memoize(ReChargeController::buildAliapyClient);

    private static DefaultAlipayClient buildAliapyClient() {
        if (!Environment.supportAlipay) {
            throw new IllegalStateException("your service not support alipay");
        }
        return new DefaultAlipayClient(ALIPAY_GATE_WAY,
                Environment.aliPayAppId,
                Environment.aliPayKeyPrivate,
                AlipayConstants.FORMAT_JSON,
                StandardCharsets.UTF_8.name(),
                Environment.aliPayKeyPublic,
                AlipayConstants.SIGN_TYPE_RSA2);
    }

    private static final Supplier<RSAAutoCertificateConfig> wechatConfig = Suppliers.memoize(ReChargeController::buildWechatPayConfig);

    private static RSAAutoCertificateConfig buildWechatPayConfig() {
        if (!Environment.supportWechatPay) {
            throw new IllegalStateException("your service not support wechatPay");
        }
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(Environment.wechatPayMerchantId)
                .privateKey(Environment.wechatPayKeyPrivate)
                .merchantSerialNumber(Environment.wechatPayMerchantSerialNumber)
                .apiV3Key(Environment.wechatPayApiV3Key)
                .build();
    }

    private static final Supplier<NativePayService> wechatPayClient = Suppliers.memoize(ReChargeController::buildWeChatPayClient);

    private static NativePayService buildWeChatPayClient() {
        return new NativePayService.Builder().config(wechatConfig.get()).build();
    }

    /**
     * 用户充值记录
     */
    @LoginRequired
    @Operation(summary = "用户充值记录")
    @GetMapping(value = "rechargeRecord")
    public CommonRes<List<RechargeRecord>> orderInfo() {
        return CommonRes.success(
                rechargeRecordMapper.selectList(new QueryWrapper<RechargeRecord>()
                        .eq(RechargeRecord.USER, AppContext.getUser().getUserName())
                        .orderByDesc(RechargeRecord.ID)
                )
        );
    }


    @LoginRequired
    @Operation(summary = "支付宝预生单")
    @GetMapping("prepare_alipay")
    @SneakyThrows
    public CommonRes<?> aliPay(@NotNull int price, @NotBlank String returnURL) {
        UserInfo user = AppContext.getUser();
        if (!user.getIsAdmin() || StringUtils.isBlank(user.getAuthenticationThirdPartyId())) {
            return CommonRes.failed("you must do authentication before user alipay online charge");
        }
        if (!Environment.supportAlipay) {
            return CommonRes.failed("your website not support alipay,please contact website admin");
        }
        if (price < 1) {
            return CommonRes.failed("charge price error");
        }

        URL url = new URL(returnURL);
        String notifyURL = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort()
                + BuildConfig.restfulApiPrefix + "/pay/aliPayBack.json";

        log.info("alipay recharge request, price:{} user:{} notifyUrl:{} returnURL:{}",
                price, user.getUserName(), notifyURL, returnURL);

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyURL);
        request.setReturnUrl(returnURL);
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", System.currentTimeMillis() + "_" + user.getId());
        bizContent.put("total_amount", price);
        bizContent.put("subject", "malenia系统充值");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());

        AlipayTradePagePayResponse response = alipayClient.get().pageExecute(request);
        if (!response.isSuccess()) {
            return CommonRes.failed(response.getSubMsg());
        }
        return CommonRes.success(response);
    }

    @Operation(summary = "支付宝支付成功回调")
    @PostMapping("aliPayBack.json")
    @SneakyThrows
    public String aliPayBack(HttpServletRequest request) {
        Map<String, String> map = ServletUtil.paramsToMap(request);
        //{"gmt_create":"2024-04-07 09:07:07","charset":"UTF-8","gmt_payment":"2024-04-07 09:07:12","notify_time":"2024-04-07 09:07:13","subject":"malenia系统充值","sign":"HFA8nUHCK+GlAI0JdlUripiRXB4Gcif70rj8YyhwRBcXvsph4mMFQ2DB/HEjlKVuTKnHXdgMNi6sGQfBQBTKwPzYbZ9OBFcsgLT4Uz+4sC90RHSkTrRZw40LaRcl+eOH0bn14nVs7ZUguGoRZAZv5dO9ZcQG+Tmut9h5/bgz97Tmz8/WVcaVzU8apzbvIbKBEq8+lGW+OD0XU7GeQJrzdV+wL9zr0sa4qgVbcR5+El7MO1yZtreFnOqwVMw3vIGvDoRd+0g426sjqdlVQEM+B84wEdvv1ttXuPOHEI4aT4INz6D1BfhJJl3jORloq+oCU9aJIcVnw1ayr0K+mufr4Q==","merchant_app_id":"2021004139610674","buyer_open_id":"045AQEPO1YzDTO5y5UZsP4XwQAaxK-2FfyZLPBF6DgGZWA0","invoice_amount":"5.00","version":"1.0","notify_id":"2024040701222090713055451477735756","fund_bill_list":"[{\"amount\":\"5.00\",\"fundChannel\":\"ALIPAYACCOUNT\"}]","notify_type":"trade_status_sync","out_trade_no":"1712451996647_1","total_amount":"5.00","trade_status":"TRADE_SUCCESS","trade_no":"2024040722001455451459222786","auth_app_id":"2021004139610674","receipt_amount":"5.00","point_amount":"0.00","buyer_pay_amount":"5.00","app_id":"2021004139610674","sign_type":"RSA2","seller_id":"2088741917397075"}
        log.info("get alipay notify: {}", JSONObject.toJSONString(map));
        String tradeNo = map.get("trade_no");
        String outTradeNo = map.get("out_trade_no");
        boolean signVerified = AlipaySignature.rsaCheckV1(map,
                Environment.aliPayKeyPublic, StandardCharsets.UTF_8.name(),
                AlipayConstants.SIGN_TYPE_RSA2);
        if (!signVerified) {
            log.warn("error alipay callback, sign Verify failed");
            return "success";
        }
        long exist = rechargeRecordMapper.selectCount(new QueryWrapper<RechargeRecord>()
                .eq(RechargeRecord.TRADE_NO, tradeNo));
        if (exist > 0) {
            log.warn("this pay record has been handled already!!");
            return "success";
        }

        String tradeStatus = request.getParameter("trade_status");
        if (!tradeStatus.equals("TRADE_SUCCESS")) {
            // not happen
            log.info("this pay record not success");
            return "success";
        }

        double totalAmount = Double.parseDouble(request.getParameter("total_amount"));
        String buyerId = request.getParameter("buyer_open_id");

        int index = outTradeNo.indexOf("_");
        long userId = Long.parseLong(outTradeNo.substring(index + 1));
        UserInfo userInfo = userInfoMapper.selectById(userId);

        userInfoService.recharge(userInfo.getUserName(), totalAmount,
                totalAmount, "alipay-" + buyerId, "__alipay", tradeNo);
        return "success";
    }

    @LoginRequired
    @Operation(summary = "微信预生单")
    @GetMapping("prepare_wechat")
    @SneakyThrows
    public CommonRes<String> wechatPay(@NotNull int price, @NotBlank String returnURL) {
        UserInfo user = AppContext.getUser();
        if (!user.getIsAdmin() || StringUtils.isBlank(user.getAuthenticationThirdPartyId())) {
            return CommonRes.failed("you must do authentication before user wechat online charge");
        }
        if (!Environment.supportWechatPay) {
            return CommonRes.failed("your website not support wechatPay,please contact website admin");
        }
        if (price < 1) {
            return CommonRes.failed("charge price error");
        }

        URL url = new URL(returnURL);
        String notifyURL = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort()
                + BuildConfig.restfulApiPrefix + "/pay/wechatPayBack";

        log.info("wechat recharge request, price:{} user:{} notifyUrl:{} returnURL:{}",
                price, user.getUserName(), notifyURL, returnURL);

        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal((int) (price * 100));
        request.setAmount(amount);
        request.setAppid(Environment.wechatPayAppId);
        request.setMchid(Environment.wechatPayMerchantId);
        request.setDescription("malenia系统充值");
        request.setNotifyUrl(notifyURL);
        request.setOutTradeNo(System.currentTimeMillis() + "_" + user.getId());
        // 调用下单方法，得到应答
        PrepayResponse response = wechatPayClient.get().prepay(request);
        return CommonRes.success(response.getCodeUrl());
    }

    @Operation(summary = "微信支付成功回调")
    @PostMapping("wechatPayBack")
    @SneakyThrows
    public ResponseEntity<?> wechatPayBack(HttpServletRequest request) {
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(request.getHeader("Wechatpay-Serial"))
                .nonce(request.getHeader("Wechatpay-Nonce"))
                .signature(request.getHeader("Wechatpay-Signature"))
                .timestamp(request.getHeader("Wechatpay-Timestamp"))
                .body(IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8))
                .build();

        Transaction transaction;
        try {
            transaction = new NotificationParser(wechatConfig.get()).parse(requestParam, Transaction.class);
        } catch (ValidationException e) {
            // 签名验证失败，返回 401 UNAUTHORIZED 状态码
            log.error("sign verification failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Transaction.TradeStateEnum tradeState = transaction.getTradeState();
        if (tradeState != Transaction.TradeStateEnum.SUCCESS) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }

        String outTradeNo = transaction.getOutTradeNo();
        String tradeNo = transaction.getTransactionId();

        long exist = rechargeRecordMapper.selectCount(new QueryWrapper<RechargeRecord>()
                .eq(RechargeRecord.TRADE_NO, tradeNo));
        if (exist > 0) {
            log.warn("this pay record has been handled already!!");
            return ResponseEntity.ok().build();
        }


        double totalAmount = transaction.getAmount().getTotal() / 100D;
        String buyerId = transaction.getPayer().getOpenid();

        int index = outTradeNo.indexOf("_");
        long userId = Long.parseLong(outTradeNo.substring(index + 1));
        UserInfo userInfo = userInfoMapper.selectById(userId);

        userInfoService.recharge(userInfo.getUserName(), totalAmount,
                totalAmount, "wechat-" + buyerId, "__wechat", tradeNo);
        return ResponseEntity.ok().build();
    }
}
