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
 * 代理资源池
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@TableName("ip_source")
@Schema(name = "IpSource", description = "代理资源池")
public class IpSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "资源key")
    private String sourceKey;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "资源解析处理器")
    private String loadResourceHandler;

    @Schema(description = "资源下载url")
    private String loadUrl;

    @Schema(description = "鉴权账户表达式")
    @Deprecated
    private String authUserNameExp;

    @Schema(description = "当前资源是否生效")
    private Boolean enabled;

    @Schema(description = "上游代理鉴权账户（解析结果没有是的默认兜底）")
    private String upUserName;

    @Schema(description = "上游代理鉴权密码")
    private String upUserPassword;

    @Schema(description = "本ip池描述")
    private String description;

    @Schema(description = "预估ip池大小")
    private Integer poolSize;

    @Schema(description = "是否需要探测可用性")
    private Boolean needTest;

    @Schema(description = "ip下载时间间隔（秒）")
    private Integer reloadInterval;

    @Schema(description = "最长存活时间（秒）")
    private Integer maxAlive;

    @Schema(description = "本ip池支持的协议")
    private String supportProtocol;

    @Schema(description = "连接池连接空转时间（秒），超时被系统回收")
    private Integer connIdleSeconds;

    @Schema(description = "链接缓存池检查和创建时间间隔，单位：秒")
    private Integer makeConnInterval;

    public static final String ID = "id";

    public static final String SOURCE_KEY = "source_key";

    public static final String CREATE_TIME = "create_time";

    public static final String LOAD_RESOURCE_HANDLER = "load_resource_handler";

    public static final String LOAD_URL = "load_url";

    public static final String AUTH_USER_NAME_EXP = "auth_user_name_exp";

    public static final String ENABLED = "enabled";

    public static final String UP_USER_NAME = "up_user_name";

    public static final String UP_USER_PASSWORD = "up_user_password";

    public static final String DESCRIPTION = "description";

    public static final String POOL_SIZE = "pool_size";

    public static final String NEED_TEST = "need_test";

    public static final String RELOAD_INTERVAL = "reload_interval";

    public static final String MAX_ALIVE = "max_alive";

    public static final String SUPPORT_PROTOCOL = "support_protocol";

    public static final String CONN_IDLE_SECONDS = "conn_idle_seconds";

    public static final String MAKE_CONN_INTERVAL = "make_conn_interval";
}
