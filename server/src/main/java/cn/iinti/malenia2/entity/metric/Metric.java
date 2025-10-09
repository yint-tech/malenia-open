package cn.iinti.malenia2.entity.metric;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.micrometer.core.instrument.Meter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * <p>
 * 监控指标
 * </p>
 *
 * @author iinti
 * @since 2023-03-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(name = "Metric对象", description = "监控指标")
public class Metric implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "指标名称")
    private String name;

    @Schema(name = "时间索引")
    private String timeKey;


    @Schema(name = "对于tag字段自然顺序拼接求md5")
    private String tagsMd5;

    @Schema(name = "指标tag")
    private String tag1;

    @Schema(name = "指标tag")
    private String tag2;

    @Schema(name = "指标tag")
    private String tag3;

    @Schema(name = "指标tag")
    private String tag4;

    @Schema(name = "指标tag")
    private String tag5;

    @Schema(name = "指标类型：（counter、gauge、timer，请注意暂时只支持这三种指标）")
    private Meter.Type type;

    @Schema(name = "指标值")
    private Double value;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;


    public static final String ID = "id";

    public static final String NAME = "name";

    public static final String TIME_KEY = "time_key";

    public static final String TAGS_MD5 = "tags_md5";

    public static final String TAG1 = "tag1";

    public static final String TAG2 = "tag2";

    public static final String TAG3 = "tag3";

    public static final String TAG4 = "tag4";

    public static final String TAG5 = "tag5";

    public static final String TYPE = "type";

    public static final String VALUE = "value";

    public static final String CREATE_TIME = "create_time";

}
