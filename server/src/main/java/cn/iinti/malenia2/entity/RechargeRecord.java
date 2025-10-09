package cn.iinti.malenia2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 充值记录表
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@TableName("recharge_record")
@Schema(name = "RechargeRecord", description = "充值记录表")
public class RechargeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "充值目标用户")
    private String user;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "充值备注")
    private String comment;

    @Schema(description = "充值金额")
    private Double rechargeAmount;

    @Schema(description = "实际支付金额")
    private Double actualPayAmount;

    @Schema(description = "充值前余额")
    private Double remainBalance;

    @Schema(description = "充值前实际支付总额")
    private Double totalActualPayAmount;

    @Schema(description = "支付流水号，支付宝|微信返回")
    private String tradeNo;

    public static final String ID = "id";

    public static final String OPERATOR = "operator";

    public static final String USER = "user";

    public static final String CREATE_TIME = "create_time";

    public static final String COMMENT = "comment";

    public static final String RECHARGE_AMOUNT = "recharge_amount";

    public static final String ACTUAL_PAY_AMOUNT = "actual_pay_amount";

    public static final String REMAIN_BALANCE = "remain_balance";

    public static final String TOTAL_ACTUAL_PAY_AMOUNT = "total_actual_pay_amount";

    public static final String TRADE_NO = "trade_no";
}
