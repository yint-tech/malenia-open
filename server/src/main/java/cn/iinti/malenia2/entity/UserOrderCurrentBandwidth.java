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
 * 订单带宽trace表，用于计算多个分布式节点的代理流量带宽
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@TableName("user_order_current_bandwidth")
@Schema(name = "UserOrderCurrentBandwidth", description = "订单带宽trace表，用于计算多个分布式节点的代理流量带宽")
public class UserOrderCurrentBandwidth implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "订单ID")
    private Long userOrderId;

    @Schema(description = "当前服务器节点ID")
    private String serverId;

    @Schema(description = "当前服务器、当前订单的带宽情况")
    private Double currentBandwidth;

    @Schema(description = "最后修改更新时间")
    private LocalDateTime lastUpdateTime;

    public static final String ID = "id";

    public static final String USER_ORDER_ID = "user_order_id";

    public static final String SERVER_ID = "server_id";

    public static final String CURRENT_BANDWIDTH = "current_bandwidth";

    public static final String LAST_UPDATE_TIME = "last_update_time";
}
