package cn.iinti.malenia2.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 产品订购关系表
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@TableName("user_order")
@Schema(name = "UserOrder", description = "产品订购关系表")
public class UserOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "产品ID")
    private String productId;

    @Schema(description = "订购用户")
    private String purchaseUser;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "结算方案")
    private EnumBalanceMethod balanceMethod;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "实际结算价格")
    private Double balancePrice;

    @Schema(description = "带宽实际结算价格备份(避免计费方式切换导致带宽价格丢失),因为修改带宽会对应修改带宽的价格")
    private Double bandwidthBalancePrice;

    @Schema(description = "推荐人")
    private String referer;

    @Schema(description = "带宽限制")
    private Double bandwidthLimit;

    @Schema(description = "随机隧道，开启后用户每个请求都会随机")
    private Boolean randomTurning;

    @Schema(description = "连接创建超时时间，超时将会由代理服务器主动502")
    private Integer connectTimeout;

    @Schema(description = "最大重试次数")
    private Integer maxFailoverCount;

    @Schema(description = "实时带宽(M)")
    @TableField(exist = false)
    private Double runtimeBandwidth;

    private static final long M = 1024 * 1024;

    public long bandwidthLimitWithByte() {
        return (long) (bandwidthLimit * M / 8);
    }

    public static final String ID = "id";

    public static final String PRODUCT_NAME = "product_name";

    public static final String PRODUCT_ID = "product_id";

    public static final String PURCHASE_USER = "purchase_user";

    public static final String CREATE_TIME = "create_time";

    public static final String BALANCE_METHOD = "balance_method";

    public static final String ENABLED = "enabled";

    public static final String BALANCE_PRICE = "balance_price";

    public static final String BANDWIDTH_BALANCE_PRICE = "bandwidth_balance_price";

    public static final String REFERER = "referer";

    public static final String BANDWIDTH_LIMIT = "bandwidth_limit";

    public static final String RANDOM_TURNING = "random_turning";

    public static final String CONNECT_TIMEOUT = "connect_timeout";

    public static final String MAX_FAILOVER_COUNT = "max_failover_count";

    @Getter
    public enum EnumBalanceMethod {
        METHOD_HOUR(0, "按时付费(小时)"),
        METHOD_FLOW(1, "按量付费(0.01G)");
        @EnumValue
        private final int method;

        private final String des;

        EnumBalanceMethod(int method, String des) {
            this.method = method;
            this.des = des;
        }
    }
}
