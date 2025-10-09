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
 * 资源产品绑定关系
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@TableName("product_source")
@Schema(name = "ProductSource", description = "资源产品绑定关系")
public class ProductSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "产品id")
    private String productId;

    @Schema(description = "资源名称")
    private String sourceKey;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "流量比例")
    private Integer ratio;

    public static final String ID = "id";

    public static final String PRODUCT_ID = "product_id";

    public static final String SOURCE_KEY = "source_key";

    public static final String CREATE_TIME = "create_time";

    public static final String RATIO = "ratio";
}
