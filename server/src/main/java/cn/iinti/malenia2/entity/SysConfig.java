package cn.iinti.malenia2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
 * 系统配置
 * </p>
 *
 * @author iinti
 * @since 2022-12-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_config")
@Schema(name = "SysConfig对象", description = "系统配置")
public class SysConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "key")
    private String configKey;

    @Schema(name = "value")
    private String configValue;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @Schema(name = "配置备注")
    private String configComment;


    public static final String ID = "id";

    public static final String CONFIG_KEY = "config_key";

    public static final String CONFIG_VALUE = "config_value";

    public static final String CREATE_TIME = "create_time";

    public static final String CONFIG_COMMENT = "config_comment";

}
