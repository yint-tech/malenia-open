package cn.iinti.malenia2.service.base;


import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.entity.*;
import cn.iinti.malenia2.mapper.RechargeRecordMapper;
import cn.iinti.malenia2.mapper.UserInfoMapper;
import cn.iinti.malenia2.mapper.UserOrderCurrentBandwidthMapper;
import cn.iinti.malenia2.mapper.UserOrderMapper;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.dbcache.DbCacheManager;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.base.perm.PermsService;
import cn.iinti.malenia2.system.AppContext;
import cn.iinti.malenia2.utils.Md5Utils;
import cn.iinti.malenia2.utils.net.SimpleHttpInvoker;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 用户信息 服务实现类
 * </p>
 *
 * @author iinti
 * @since 2022-02-22
 */
@Service
@Slf4j
public class UserInfoService {

    @Resource
    private UserInfoMapper userMapper;

    @Resource
    private DbCacheManager dbCacheManager;
    @Resource
    private UserOrderMapper userOrderMapper;

    @Resource
    private UserOrderCurrentBandwidthMapper userOrderCurrentBandwidthMapper;

    @Resource
    private RechargeRecordMapper rechargeRecordMapper;

    private static UserInfoService instance;

    public UserInfoService() {
        instance = this;
    }


    private static final String salt = BuildConfig.appName + "2023V2!@&*(";

    public CommonRes<UserInfo> login(String account, String password) {
        UserInfo userInfo = userMapper
                .selectOne(new QueryWrapper<UserInfo>().eq(UserInfo.USER_NAME, account).last("limit 1"));
        if (userInfo == null) {
            return CommonRes.failed("请检查用户名或密码");
        }
        if (!StringUtils.equals(userInfo.getPassword(), password)) {
            return CommonRes.failed("请检查用户名或密码");
        }

        userInfo.setLoginToken(genLoginToken(userInfo, LocalDateTime.now()));
        userMapper.update(null, new UpdateWrapper<UserInfo>().eq(UserInfo.USER_NAME, userInfo.getUserName())
                .set(UserInfo.LOGIN_TOKEN, userInfo.getLoginToken()));
        return CommonRes.success(userInfo);
    }

    public CommonRes<UserInfo> resetUserPassword(Long userId, String password) {
        if (StringUtils.isBlank(password)) {
            return CommonRes.failed("密码格式不正确。");
        }
        UserInfo userInfo = userMapper
                .selectOne(new QueryWrapper<UserInfo>().eq(UserInfo.ID, userId).last("limit 1"));
        if (userInfo == null) {
            return CommonRes.failed("用户不存在");
        }
        userInfo.setPassword(password);
        userMapper.update(null, new UpdateWrapper<UserInfo>().eq(UserInfo.USER_NAME, userInfo.getUserName()).set(UserInfo.PASSWORD, password));
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return CommonRes.success(userInfo);
    }

    public String refreshToken(String oldToken) {
        UserInfo userInfo = getUserInfoFromToken(oldToken);
        if (userInfo == null) {
            //token不合法
            return null;
        }
        if (isRightToken(oldToken, userInfo)) {
            return genLoginToken(userInfo, LocalDateTime.now());
        }

        return null;
    }

    private boolean isRightToken(String token, UserInfo userInfo) {
        for (int i = 0; i < 3; i++) {

            // 每个token三分钟有效期，算法检测历史9分钟内的token，超过9分钟没有执行刷新操作，token失效
            String historyToken = genLoginToken(userInfo, LocalDateTime.now().minusMinutes(i * 3));
            if (historyToken.equals(token)) {
                return true;
            }
        }
        return false;
    }

    public String genLoginToken(UserInfo userInfo, LocalDateTime date) {
        byte[] bytes = Md5Utils.md5Bytes(userInfo.getUserName() + "|" + userInfo.getPassword() + "|" + salt + "|" + (
                date.get(ChronoField.MINUTE_OF_DAY) / 30) + "|" + date.getDayOfYear());
        //
        byte[] userIdData = longToByte(userInfo.getId());
        byte[] finalData = new byte[bytes.length + userIdData.length];

        for (int i = 0; i < 8; i++) {
            finalData[i * 2] = userIdData[i];
            finalData[i * 2 + 1] = bytes[i];
        }

        if (bytes.length - 8 >= 0) {
            System.arraycopy(bytes, 8, finalData, 16, bytes.length - 8);
        }

        return Md5Utils.toHexString(finalData);
    }

    public static long byteToLong(byte[] b) {
        long s;
        long s0 = b[0] & 0xff;// 最低位
        long s1 = b[1] & 0xff;
        long s2 = b[2] & 0xff;
        long s3 = b[3] & 0xff;
        long s4 = b[4] & 0xff;// 最低位
        long s5 = b[5] & 0xff;
        long s6 = b[6] & 0xff;
        long s7 = b[7] & 0xff;

        // s0不变
        s1 <<= 8;
        s2 <<= 16;
        s3 <<= 24;
        s4 <<= 8 * 4;
        s5 <<= 8 * 5;
        s6 <<= 8 * 6;
        s7 <<= 8 * 7;
        s = s0 | s1 | s2 | s3 | s4 | s5 | s6 | s7;
        return s;
    }

    public static byte[] longToByte(long number) {
        long temp = number;
        byte[] b = new byte[8];
        for (int i = 0; i < b.length; i++) {
            b[i] = Long.valueOf(temp & 0xff).byteValue();
            // 将最低位保存在最低位
            temp = temp >> 8;
            // 向右移8位
        }
        return b;
    }


    // 用户名中非法的字符,这是因为我们的系统将会直接使用用户名称做业务，一些奇怪的字符串将会引起一些紊乱
    // 如鉴权表达式：传递控制信息
    //   资产文件：使用用户名隔离多个用户的文件存储
    private static final char[] illegalUserNameChs = " -/\t\n*\\".toCharArray();

    public CommonRes<UserInfo> register(String account, String password) {
        if (StringUtils.isAnyBlank(account, password)) {
            return CommonRes.failed("用户或者密码不能为空");
        }
        if (StringUtils.containsAny(account, illegalUserNameChs)) {
            return CommonRes.failed("userName contain illegal character");
        }
        UserInfo userInfo = userMapper
                .selectOne(new QueryWrapper<UserInfo>().eq(UserInfo.USER_NAME, account).last("limit 1"));

        if (userInfo != null) {
            return CommonRes.failed("用户已存在");
        }

        Long adminCount = userMapper.selectCount(new QueryWrapper<UserInfo>().eq(UserInfo.IS_ADMIN, true));

        boolean isCallFromAdmin = AppContext.getUser() != null && AppContext.getUser().getIsAdmin();
        if (!isCallFromAdmin && adminCount != 0 && !Settings.allowRegisterUser.value) {
            return CommonRes.failed("当前系统不允许注册新用户，详情请联系管理员");
        }

        userInfo = new UserInfo();
        userInfo.setUserName(account);
        userInfo.setPassword(password);
        userInfo.setApiToken(UUID.randomUUID().toString());
        // 第一个注册用户，认为是管理员
        userInfo.setIsAdmin(adminCount == 0);

        int result = userMapper.insert(userInfo);
        if (result == 1) {
            userInfo.setLoginToken(genLoginToken(userInfo, LocalDateTime.now()));
            userMapper.update(null, new UpdateWrapper<UserInfo>().eq(UserInfo.USER_NAME, userInfo.getUserName())
                    .set(UserInfo.LOGIN_TOKEN, userInfo.getLoginToken()));
            BroadcastService.triggerEvent(BroadcastService.Topic.USER);
            return CommonRes.success(userInfo);
        }

        return CommonRes.failed("注册失败，请稍后再试");
    }


    public CommonRes<UserInfo> checkAPIToken(List<String> tokenList) {
        for (String token : tokenList) {
            UserInfo userInfo = dbCacheManager.getUserCacheWithApiToken().getModeWithCache(token);
            if (userInfo != null) {
                return CommonRes.success(userInfo);
            }
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        if (tokenList.size() == 1) {
            queryWrapper.eq(UserInfo.API_TOKEN, tokenList.get(0));
        } else {
            queryWrapper.in(UserInfo.API_TOKEN, tokenList);
        }

        UserInfo userInfo = userMapper.selectOne(queryWrapper.last("limit 1"));
        if (userInfo != null) {
            return CommonRes.success(userInfo);
        }
        return CommonRes.failed("请登录");
    }

    public CommonRes<UserInfo> checkLogin(List<String> tokenList) {
        for (String candidateToken : tokenList) {
            CommonRes<UserInfo> res = checkLogin(candidateToken);
            if (res.isOk()) {
                return res;
            }
        }
        return CommonRes.failed("请登录");
    }

    public CommonRes<UserInfo> checkLogin(String token) {
        UserInfo userInfo = getUserInfoFromToken(token);
        if (userInfo == null) {
            return CommonRes.failed(CommonRes.statusNeedLogin, "token错误");
        }
        if (!isRightToken(token, userInfo)) {
            return CommonRes.failed("请登录");
        }
        userInfo.setLoginToken(genLoginToken(userInfo, LocalDateTime.now()));
        userMapper.update(null, new UpdateWrapper<UserInfo>()
                .eq(UserInfo.USER_NAME, userInfo.getUserName())
                .set(UserInfo.LOGIN_TOKEN, userInfo.getLoginToken())
        );
        return CommonRes.success(userInfo);
    }


    public static CommonRes<UserInfo> staticCheckLogin(String token) {
        return instance.checkLogin(token);
    }

    private UserInfo getUserInfoFromToken(String token) {
        // check token format
        if (StringUtils.isBlank(token)) {
            return null;
        }
        if ((token.length() & 0x01) != 0) {
            //token长度必须是偶数
            return null;
        }
        if (token.length() < 16) {
            return null;
        }
        for (char ch : token.toCharArray()) {
            // [0-9] [a-f]
            if (ch >= '0' && ch <= '9') {
                continue;
            }
            if (ch >= 'a' && ch <= 'f') {
                continue;
            }
            //log.warn("broken token: {} reason:none dex character", token);
            return null;
        }

        byte[] bytes = Md5Utils.hexToByteArray(token);
        byte[] longByteArray = new byte[8];
        // byte[] md5BeginByteArray = new byte[8];
        for (int i = 0; i < 8; i++) {
            longByteArray[i] = bytes[i * 2];
            //  md5BeginByteArray[i] = bytes[i * 2 + 1];
        }
        long userId = byteToLong(longByteArray);
        UserInfo userInfo = dbCacheManager.getUserCacheWithId().getModeWithCache(String.valueOf(userId));
        if (userInfo != null) {
            return userInfo;
        }
        return userMapper.selectById(userId);
    }

    public boolean isUserExist(String username) {
        return dbCacheManager.getUserCacheWithName().getModeWithCache(username) != null;
    }


    public CommonRes<String> grantAdmin(String userName, boolean isAdmin) {
        UserInfo userInfo = userMapper.selectOne(new QueryWrapper<UserInfo>().eq(UserInfo.USER_NAME, userName));
        if (userInfo == null) {
            return CommonRes.failed("user not exist");
        }
        if (userInfo.getId().equals(AppContext.getUser().getId())) {
            return CommonRes.failed("you can not operate yourself");
        }
        userInfo.setIsAdmin(isAdmin);
        userMapper.update(null, new UpdateWrapper<UserInfo>().eq(UserInfo.USER_NAME, userInfo.getUserName())
                .set(UserInfo.IS_ADMIN, isAdmin));
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return CommonRes.success("ok");
    }


    public CommonRes<UserInfo> editUserPerm(String userName, String permsConfig) {
        return CommonRes.ofPresent(dbCacheManager.getUserCacheWithName().getModeWithCache(userName))
                .ifOk(userInfo -> {
                    Map<String, Collection<String>> map = PermsService.parseExp(permsConfig, false);
                    String newConfig = PermsService.rebuildExp(map);
                    userMapper.update(null, new UpdateWrapper<UserInfo>()
                            .eq(UserInfo.ID, userInfo.getId())
                            .set(UserInfo.PERMISSION, newConfig)
                    );
                    BroadcastService.triggerEvent(BroadcastService.Topic.USER);
                }).transform(userInfo -> userMapper.selectById(userInfo.getId()));

    }

    @Transactional
    public Double recharge(String user, double amount, double actualPayAmount, String rechargeComment,
                           String operator, String tradeNo) {
        if (StringUtils.isBlank(user)) {
            throw new IllegalStateException("充值用户不能为空或金额不合法");
        }
        UserInfo userInfo = userMapper.selectOne(new QueryWrapper<UserInfo>().eq(UserInfo.USER_NAME, user));
        if (userInfo == null) {
            throw new IllegalStateException("充值用户不存在");
        }

        Double preBalance = userInfo.getBalance();
        userInfo.setBalance(userInfo.getBalance() + amount);
        userInfo.setActualPayAmount(userInfo.getActualPayAmount() + actualPayAmount);
        int res = userMapper.update(null,
                new UpdateWrapper<UserInfo>().eq(UserInfo.USER_NAME, user)
                        .eq(UserInfo.BALANCE, preBalance)
                        .set(UserInfo.BALANCE, userInfo.getBalance())
                        .set(UserInfo.ACTUAL_PAY_AMOUNT, userInfo.getActualPayAmount()));
        if (res > 0) {
            // 充值记录
            if (StringUtils.isBlank(operator)) {
                UserInfo contextUser = AppContext.getUser();
                operator = contextUser == null ? "system" : contextUser.getUserName();
            }
            recordRecharge(operator, userInfo, amount, actualPayAmount, rechargeComment, tradeNo);
        }
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
        return userInfo.getBalance();
    }

    private void recordRecharge(String operator, UserInfo userInfo, double rechargeAmount, double actualPayAmount,
                                String comment, String tradeNo) {
        RechargeRecord rechargeRecord = new RechargeRecord();
        rechargeRecord.setOperator(operator);
        rechargeRecord.setActualPayAmount(actualPayAmount);
        rechargeRecord.setRechargeAmount(rechargeAmount);
        rechargeRecord.setTotalActualPayAmount(userInfo.getActualPayAmount());
        rechargeRecord.setRemainBalance(userInfo.getBalance());
        rechargeRecord.setUser(userInfo.getUserName());
        rechargeRecord.setComment(comment);
        rechargeRecord.setTradeNo(tradeNo);
        rechargeRecordMapper.insert(rechargeRecord);
    }

    public CommonRes<Boolean> setOrderBandwidthLimit(long orderId, double bandwidthLimit, double newOrderPrice) {
        if (bandwidthLimit <= 1) {
            bandwidthLimit = 1;
        }

        UserOrder userOrder = userOrderMapper.selectById(orderId);
        if (userOrder == null) {
            return CommonRes.failed("record not exist");
        }

        if (userOrder.getBalanceMethod() == UserOrder.EnumBalanceMethod.METHOD_FLOW) {
            return CommonRes.failed("按流量付费模式不需要设置带宽");
        }

        userOrder.setBandwidthLimit(bandwidthLimit);
        userOrder.setBalancePrice(newOrderPrice);
        userOrder.setBandwidthBalancePrice(newOrderPrice);
        userOrderMapper.updateById(userOrder);

        // reset current bandwidth to fast growth order bandwidth limit settings
        userOrderCurrentBandwidthMapper.update(null, new UpdateWrapper<UserOrderCurrentBandwidth>()
                .eq(UserOrderCurrentBandwidth.USER_ORDER_ID, orderId)
                .set(UserOrderCurrentBandwidth.CURRENT_BANDWIDTH, 0)
        );
        return CommonRes.success(true);
    }


    // 个人认证
    private static final String PERSONAL = "个人";
    private static final String PERSONAL_URL_PASS = "01";
    private static final Pattern NUMBER_REGEX = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final Pattern CREDIT_CODE_REGEX = Pattern.compile("^[0-9A-Z]{18}$");

    public CommonRes<String> doAuthentication(UserInfo userInfo, boolean manual) {
        if (manual) {
            if (userInfo.getId() == null) {
                return CommonRes.failed("must userId in manual authentication model");
            }
        } else {
            UserInfo sessionUser = AppContext.getUser();
            userInfo.setId(sessionUser.getId());
            userInfo.setUserName(sessionUser.getUserName());
        }
        String authenticationType = userInfo.getAuthenticationType();
        CommonRes<String> ret;
        if (StringUtils.containsIgnoreCase(authenticationType, "person")) {
            ret = doPersonCertification(userInfo, manual);
        } else {
            ret = doEnterpriseCertification(userInfo, manual);
        }
        ret.ifOk(s -> BroadcastService.triggerEvent(BroadcastService.Topic.USER));
        return ret;
    }


    private static final String personalUrl = "https://idcert.market.alicloudapi.com/idcard";


    private static final String enterpriseUrl = "https://smkjqysys.market.alicloudapi.com/company-three/check/v2";

    private CommonRes<String> doEnterpriseCertification(UserInfo userInfo, boolean manual) {
        //https://market.aliyun.com/products/56928005/cmapi00064074.html
        String companyName = userInfo.getAuthenticationCompanyName();
        String licenseNumber = userInfo.getAuthenticationLicenseNumber();
        String corporateName = userInfo.getAuthenticationCorporateName();

        if (StringUtils.isAnyBlank(companyName,
                licenseNumber, corporateName)) {
            return CommonRes.failed("企业认证信息不能为空!");
        }
        Matcher matcher = CREDIT_CODE_REGEX.matcher(licenseNumber);
        if (!matcher.matches()) {
            return CommonRes.failed("统一社会信用代码格式不正确!");
        }

        long existCount = userMapper.selectCount(new QueryWrapper<UserInfo>()
                .eq(UserInfo.AUTHENTICATION_LICENSE_NUMBER, licenseNumber)
        );
        if (existCount > 0) {
            return CommonRes.failed("该社会信用代码已经被使用。");
        }

        UpdateWrapper<UserInfo> baseUpdateWrapper = new UpdateWrapper<UserInfo>()
                .eq(UserInfo.ID, userInfo.getId())
                .set(UserInfo.AUTHENTICATION_COMPANY_NAME, companyName)
                .set(UserInfo.AUTHENTICATION_LICENSE_NUMBER, licenseNumber)
                .set(UserInfo.AUTHENTICATION_CORPORATE_NAME, corporateName)
                .set(UserInfo.AUTHENTICATION_COMMENT, userInfo.getAuthenticationComment());

        if (manual) {
            // 通过验证
            userMapper.update(null, baseUpdateWrapper
                    .set(UserInfo.AUTHENTICATION_TYPE, "manual-enterprise")
                    .set(UserInfo.AUTHENTICATION_THIRD_PARTY_ID, UUID.randomUUID().toString())
            );
            return CommonRes.success("ok");
        }
        SimpleHttpInvoker.addHeader("Authorization", "APPCODE " + Environment.enterpriseAppcode);
        String response = SimpleHttpInvoker.get(enterpriseUrl, new HashMap<String, String>() {{
            put("companyName", companyName);
            put("creditNo", licenseNumber);
            put("legalPerson", corporateName);
        }});

        log.info("call aliyun response: {}", response);
        if (response == null) {
            log.error("call aliyun api error: ", SimpleHttpInvoker.getIoException());
            return CommonRes.failed("系统异常，请联系管理员!");
        }

        JSONObject jsonObject = JSONObject.parseObject(response);
        if (200 != jsonObject.getIntValue("code")) {
            return CommonRes.failed(jsonObject.getString("msg"));
        }
        // 返回成功,需要判断result
        // "result": //验证结果码 0-不一致；1-一致；2-输⼊企业名疑似曾⽤名，其他两要素⼀致；3-无数据
        JSONObject data = jsonObject.getJSONObject("data");
        if (data.getIntValue("result") != 1) {
            return CommonRes.failed(data.getString("resultMsg"));
        }

        // 通过验证
        userMapper.update(null, baseUpdateWrapper
                .set(UserInfo.AUTHENTICATION_TYPE, "auto-enterprise")
                .set(UserInfo.AUTHENTICATION_THIRD_PARTY_ID, data.getString("orderNo"))
        );
        return CommonRes.success("ok");
    }


    public CommonRes<String> doPersonCertification(UserInfo userInfo, boolean manual) {
        // https://market.aliyun.com/products/57000002/cmapi022049.html
        String idCard = userInfo.getAuthenticationIdCard();
        String realName = userInfo.getAuthenticationRealName();
        if (StringUtils.isBlank(idCard) || !NUMBER_REGEX.matcher(idCard).matches()) {
            return CommonRes.failed("身份证号码格式不正确!");
        }
        if (StringUtils.isBlank(realName)) {
            return CommonRes.failed("姓名不能为空!");
        }
        long existCount = userMapper.selectCount(new QueryWrapper<UserInfo>()
                .eq(UserInfo.AUTHENTICATION_ID_CARD, idCard)
        );
        if (existCount > 0) {
            return CommonRes.failed("该证件号已经被使用。");
        }
        UpdateWrapper<UserInfo> baseUpdateWrapper = new UpdateWrapper<UserInfo>()
                .eq(UserInfo.ID, userInfo.getId())
                .set(UserInfo.AUTHENTICATION_ID_CARD, idCard)
                .set(UserInfo.AUTHENTICATION_REAL_NAME, realName)
                .set(UserInfo.AUTHENTICATION_COMMENT, userInfo.getAuthenticationComment());
        if (manual) {
            // 通过验证
            userMapper.update(null, baseUpdateWrapper
                    .set(UserInfo.AUTHENTICATION_TYPE, "manual-person")
                    .set(UserInfo.AUTHENTICATION_THIRD_PARTY_ID, UUID.randomUUID().toString())
            );
            return CommonRes.success("ok");
        }
        //格式Authorization:APPCODE (中间是英文空格)
        SimpleHttpInvoker.addHeader("Authorization", "APPCODE " + Environment.personalAppcode);
        String response = SimpleHttpInvoker.get(personalUrl, new HashMap<String, String>() {{
            put("idCard", idCard);
            put("name", realName);
        }});
        if (response == null) {
            String errorMsg = SimpleHttpInvoker.getResponseHeader("X-Ca-Error-Message");
            errorMsg = StringUtils.defaultString(errorMsg, "networkError");
            log.error("call aliyun api error: " + errorMsg, SimpleHttpInvoker.getIoException());
            return CommonRes.failed(errorMsg);
        }
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (!PERSONAL_URL_PASS.equals(jsonObject.getString("status"))) {
            throw new RuntimeException(jsonObject.getString("msg"));
        }
        // 通过验证
        userMapper.update(null, baseUpdateWrapper
                .set(UserInfo.AUTHENTICATION_TYPE, "auto-person")
                .set(UserInfo.AUTHENTICATION_THIRD_PARTY_ID, jsonObject.getString("traceId"))
        );
        return CommonRes.success("ok");
    }
}
