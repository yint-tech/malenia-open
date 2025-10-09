package cn.iinti.malenia2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 对账单，记录各个用户和产品的使用详情，按小时为维度进行统计
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@Schema(name = "Bill", description = "对账单，记录各个用户和产品的使用详情，按小时为维度进行统计")
public class Bill extends EntityBase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "产品ID")
    private String productId;

    @Schema(description = "订购用户")
    private String purchaseUser;

    @Schema(description = "结算方案")
    private UserOrder.EnumBalanceMethod balanceMethod;

    @Schema(description = "实际结算价格")
    private Double balancePrice;

    @Schema(description = "结算时间，精确到小时")
    private String billTime;

    @Schema(description = "消费的金额")
    private Double consumeAmount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "扣费服务器节点")
    private String opNode;

    @Schema(description = "当前账户余额")
    private Double userBalance;

    public static final String PRODUCT_NAME = "product_name";

    public static final String PRODUCT_ID = "product_id";

    public static final String PURCHASE_USER = "purchase_user";

    public static final String BALANCE_METHOD = "balance_method";

    public static final String BALANCE_PRICE = "balance_price";

    public static final String BILL_TIME = "bill_time";

    public static final String CONSUME_AMOUNT = "consume_amount";

    public static final String CREATE_TIME = "create_time";

    public static final String OP_NODE = "op_node";

    public static final String USER_BALANCE = "user_balance";
}
