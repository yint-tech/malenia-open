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
 * 服务器节点，多台服务器组成代理集群
 * </p>
 *
 * @author iinti
 * @since 2022-12-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("server_node")
@Schema(name = "ServerNode对象", description = "服务器节点，多台服务器组成代理集群")
public class ServerNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "服务器id，唯一标记服务器")
    private String serverId;

    @Schema(name = "最后心跳时间")
    private LocalDateTime lastActiveTime;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;

    @Schema(name = "工作IP")
    private String ip;

    @Schema(name = "springboot 服务开启端口")
    private Integer port;

    @Schema(name ="服务器是否启用")
    private Boolean enable;

    @Schema(name = "本地ip")
    private String localIp;

    @Schema(name = "工作ip")
    private String outIp;


    public static final String ID = "id";

    public static final String SERVER_ID = "server_id";

    public static final String LAST_ACTIVE_TIME = "last_active_time";

    public static final String CREATE_TIME = "create_time";


    public static final String IP = "ip";

    public static final String PORT = "port";
    public static final String ENABLE = "enable";

    public static final String LOCAL_IP = "local_ip";

    public static final String OUT_IP = "out_ip";
}
