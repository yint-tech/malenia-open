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
 * 出口ip白名单
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@TableName("auth_white_ip")
@Schema(name = "AuthWhiteIp", description = "出口ip白名单")
public class AuthWhiteIp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "出口ip")
    private String whiteIp;

    @Schema(description = "对应的账户")
    private String bindUser;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String comment;

    @Schema(description = "是否来自API配置")
    private Boolean fromApi;

    public static final String ID = "id";

    public static final String WHITE_IP = "white_ip";

    public static final String BIND_USER = "bind_user";

    public static final String CREATE_TIME = "create_time";

    public static final String COMMENT = "comment";

    public static final String FROM_API = "from_api";
}
