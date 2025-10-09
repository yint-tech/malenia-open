package cn.iinti.malenia2.entity.metric;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 监控指标,小时级
 * </p>
 *
 * @author iinti
 * @since 2023-03-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("metric_hour")
@Schema(name =  "Metric对象", description = "监控指标")
public class MetricHour extends Metric {


}
