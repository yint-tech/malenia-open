package cn.iinti.malenia2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 用户访问日志
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@TableName("access_record")
@Schema(name = "AccessRecord", description = "用户访问日志")
public class AccessRecord extends EntityBase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "对应的账户")
    private String accessUser;

    @Schema(description = "访问的目标网址")
    private String targetHost;

    @Schema(description = "结算时间，精确到小时")
    private String recordTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "任务id，用于快速查询")
    private String recordMd5;

    @Schema(description = "访问次数")
    private Integer accessCount;


    public static final String ACCESS_USER = "access_user";

    public static final String TARGET_HOST = "target_host";

    public static final String RECORD_TIME = "record_time";

    public static final String CREATE_TIME = "create_time";

    public static final String RECORD_MD5 = "record_md5";

    public static final String ACCESS_COUNT = "access_count";
}
