package cn.iinti.malenia2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户信息
 * </p>
 *
 * @author iinti
 * @since 2022-12-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_info")
@Schema(name = "UserInfo对象", description = "用户信息")
public class UserInfo extends EntityBase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "用户名")
    private String userName;

    @Schema(name = "密码")
    private String password;

    @Schema(name = "最后登陆时间")
    private LocalDateTime lastActive;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @Schema(name = "登录token")
    private String loginToken;

    @Schema(name = "api 访问token")
    private String apiToken;

    @Schema(name = "是否是管理员")
    private Boolean isAdmin;

    @Schema(name = "最后更新时间")
    private LocalDateTime updateTime;

    @Schema(name = "用户权限")
    private String permission;

    @Schema(name = "账户余额")
    private Double balance;

    @Schema(name = "代理鉴权账户")
    private String authAccount;

    @Schema(name = "代理鉴权密码")
    private String authPwd;

    @Schema(name = "实际支付金额")
    private Double actualPayAmount;

    @Schema(name = "实名认证：真实姓名")
    private String authenticationRealName;

    @Schema(name = "实名认证：身份证")
    private String authenticationIdCard;

    @Schema(name = "实名认证：企业名称")
    private String authenticationCompanyName;

    @Schema(name = "实名认证：营业执照号码")
    private String authenticationLicenseNumber;

    @Schema(name = "实名认证：法人姓名")
    private String authenticationCorporateName;

    @Schema(name = "实名认证：认证类型,个人,企业")
    private String authenticationType;

    @Schema(name = "实名认证：第三方请求id")
    private String authenticationThirdPartyId;

    @Schema(name = "实名认证：备注")
    private String authenticationComment;

    public static final String USER_NAME = "user_name";

    public static final String PASSWORD = "password";

    public static final String LAST_ACTIVE = "last_active";

    public static final String CREATE_TIME = "create_time";

    public static final String LOGIN_TOKEN = "login_token";

    public static final String API_TOKEN = "api_token";

    public static final String IS_ADMIN = "is_admin";

    public static final String UPDATE_TIME = "update_time";

    public static final String PERMISSION = "permission";

    public static final String BALANCE = "balance";

    public static final String AUTH_ACCOUNT = "auth_account";

    public static final String AUTH_PWD = "auth_pwd";

    public static final String ACTUAL_PAY_AMOUNT = "actual_pay_amount";

    public static final String AUTHENTICATION_REAL_NAME = "authentication_real_name";

    public static final String AUTHENTICATION_ID_CARD = "authentication_id_card";

    public static final String AUTHENTICATION_COMPANY_NAME = "authentication_company_name";

    public static final String AUTHENTICATION_LICENSE_NUMBER = "authentication_license_number";

    public static final String AUTHENTICATION_CORPORATE_NAME = "authentication_corporate_name";

    public static final String AUTHENTICATION_TYPE = "authentication_type";

    public static final String AUTHENTICATION_THIRD_PARTY_ID = "authentication_third_party_id";

    public static final String AUTHENTICATION_COMMENT = "authentication_comment";

}
