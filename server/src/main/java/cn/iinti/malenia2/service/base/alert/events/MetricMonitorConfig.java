package cn.iinti.malenia2.service.base.alert.events;

import cn.iinti.malenia2.service.base.alert.MetricMonitorHandle;
import cn.iinti.malenia2.service.base.metric.MetricEnums;
import cn.iinti.malenia2.service.base.metric.MetricVo;
import cn.iinti.malenia2.service.base.metric.mql.MQL;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MetricMonitorConfig {
    @Getter
    private String _id = UUID.randomUUID().toString();
    private CronExpression _cron;
    private MQL _mql;
    private LocalDateTime _start;
    private LocalDateTime _end;
    private MetricEnums.MetricAccuracy _accuracy = MetricEnums.MetricAccuracy.hours;

    private Closure<?> callback;

    public void id(String id) {
        this._id = id;
    }

    public String getId() {
        return this._id;
    }

    public void cron(String con) {
        this._cron = CronExpression.parse(con);
    }

    public void mql(String mql) {
        this._mql = MQL.compile(mql);
    }

    public void start(LocalDateTime start) {
        this._start = start;
    }

    public void end(LocalDateTime end) {
        this._end = end;
    }

    public void accuracy(MetricEnums.MetricAccuracy accuracy) {
        this._accuracy = accuracy;
    }

    @AllArgsConstructor
    public static class MetricData {
        private final Map<String, List<MetricVo>> metric;

        public double getMetricValue(String key, String... tags) {
            Double value = getMetric(key, tags);
            return value != null ? value : 0.0;
        }

        public Double getMetric(String key, String... tags) {
            List<MetricVo> metricVos = metric.get(key);
            if (metricVos == null || metricVos.isEmpty()) {
                return null;
            }
            if (metricVos.size() == 1) {
                return metricVos.get(0).getValue();
            }
            // filter by tags
            Map<String, String> tagMap = new HashMap<>();
            for (int i = 0; i < tags.length; i += 2) {
                tagMap.put(tags[i], tags[i + 1]);
            }
            for (MetricVo metricVo : metricVos) {
                if (isTagEquals(metricVo.getTags(), tagMap)) {
                    return metricVo.getValue();
                }
            }
            // warning
            return null;
        }

        private static boolean isTagEquals(Map<String, String> tags1, Map<String, String> tags2) {
            if (tags1.size() != tags2.size()) {
                return false;
            }
            for (Map.Entry<String, String> entry : tags1.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if (!StringUtils.equals(v, tags2.get(k))) {
                    return false;
                }
            }
            return true;
        }
    }

    public void valid() {
        if (_mql == null) {
            throw new IllegalStateException("mql not set");
        }
        if (_cron == null) {
            throw new IllegalStateException("cron not set");
        }
        if (callback == null) {
            throw new IllegalStateException("onMetric callback is null");
        }
    }

    public void onMetric(@DelegatesTo(MetricData.class) Closure<?> closure) {
        callback = closure;
    }

    public void fillMeta(MetricMonitorHandle metricMonitorHandle) {
        metricMonitorHandle.setMql(_mql);
        metricMonitorHandle.setCron(_cron);
        metricMonitorHandle.setCallback(callback);
        metricMonitorHandle.setStart(_start);
        metricMonitorHandle.setEnd(_end);
        metricMonitorHandle.setAccuracy(_accuracy);
    }
}

