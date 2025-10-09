package cn.iinti.malenia2.entity.metric;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

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
@Schema(name =  "Metric对象", description = "监控指标")
@TableName("metric_day")
public class MetricDay extends Metric {

}
