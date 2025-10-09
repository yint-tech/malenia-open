package cn.iinti.malenia2.entity;

import cn.iinti.malenia2.entity.type.TuningParamTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 代理产品
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@Schema(name = "Product", description = "代理产品")
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "产品ID")
    private String productId;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "该产品映射的端口范围")
    private String mappingPortSpace;

    @Schema(description = "产品的标签表达式,规定和约束了下游选择资源的算法,规定了资源类型mapping顺序")
    private String supportTag;

    @Schema(description = "当前产品是否生效")
    private Boolean enabled;

    @Schema(description = "是否是私有ip")
    private Boolean privateProduct;

    @Schema(description = "使用价格，按小时付费")
    private Double hourPrice;

    @Schema(description = "使用价格，按流量付费(G)")
    private Double flowPrice;

    @Schema(description = "产品描述")
    private String description;

    @Schema(description = "产品特征标签，可以在前端被快速筛选")
    private String features;

    @Schema(description = "隧道路由参数(需要上游支持)")
    @TableField(value = TUNING_PARAM, typeHandler = TuningParamTypeHandler.class)
    private TuningParam tuningParam;

    public static final String ID = "id";

    public static final String PRODUCT_ID = "product_id";

    public static final String PRODUCT_NAME = "product_name";

    public static final String CREATE_TIME = "create_time";

    public static final String MAPPING_PORT_SPACE = "mapping_port_space";

    public static final String SUPPORT_TAG = "support_tag";

    public static final String ENABLED = "enabled";

    public static final String PRIVATE_PRODUCT = "private_product";

    public static final String HOUR_PRICE = "hour_price";

    public static final String FLOW_PRICE = "flow_price";

    public static final String DESCRIPTION = "description";

    public static final String FEATURES = "features";

    public static final String TUNING_PARAM = "tuning_param";
}
