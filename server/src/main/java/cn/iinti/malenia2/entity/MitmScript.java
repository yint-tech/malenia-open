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
 * MTIM脚本，给用户做逻辑扩展
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@TableName("mitm_script")
@Schema(name = "MitmScript", description = "MTIM脚本，给用户做逻辑扩展")
public class MitmScript implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属用户")
    private String user;

    @Schema(description = "脚本名称（必须传）")
    private String name;

    @Schema(description = "脚本内容")
    private String content;

    @Schema(description = "优先级，数字越小优先级越高")
    private Byte priority;

    @Schema(description = "当前是否生效")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    public static final String ID = "id";

    public static final String USER = "user";

    public static final String NAME = "name";

    public static final String CONTENT = "content";

    public static final String PRIORITY = "priority";

    public static final String ENABLED = "enabled";

    public static final String CREATE_TIME = "create_time";

    public static final String UPDATE_TIME = "update_time";
}
