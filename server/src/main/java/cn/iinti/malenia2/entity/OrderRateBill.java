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
 * 订单带宽用量流水
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@TableName("order_rate_bill")
@Schema(name = "OrderRateBill", description = "订单带宽用量流水")
public class OrderRateBill implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "对账时间，精确到分钟")
    private String timeKey;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "带宽用量")
    private Long rateUsage;

    public static final String ID = "id";

    public static final String ORDER_ID = "order_id";

    public static final String TIME_KEY = "time_key";

    public static final String CREATE_TIME = "create_time";

    public static final String RATE_USAGE = "rate_usage";
}
