package cn.iinti.malenia2.service.base.alert;

import cn.iinti.malenia2.service.base.alert.events.MetricMonitorConfig;
import cn.iinti.malenia2.service.base.metric.MetricEnums;
import cn.iinti.malenia2.service.base.metric.MetricService;
import cn.iinti.malenia2.service.base.metric.MetricVo;
import cn.iinti.malenia2.service.base.metric.mql.Context;
import cn.iinti.malenia2.service.base.metric.mql.MQL;
import groovy.lang.Closure;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Setter
public class MetricMonitorHandle {
    private MQL mql;
    private CronExpression cron;
    private LocalDateTime start;
    private LocalDateTime end;
    private MetricEnums.MetricAccuracy accuracy;
    private Closure<?> callback;

    private LocalDateTime lastRun;

    public void evaluate(MetricService metricService, boolean force) {
        if (!force && !isCronRunTime()) {
            return;
        }
        if (accuracy == null) {
            accuracy = MetricEnums.MetricAccuracy.hours;
        }
        if (start != null) {
            trimAccuracy();
        }
        lastRun = LocalDateTime.now();
        Map<String, Context.MQLVar> metricData = mql.run(accuracy, metricService);
        Map<String, List<MetricVo>> map = filterAndMerge(metricData);

        MetricMonitorConfig.MetricData delegate = new MetricMonitorConfig.MetricData(map);
        callback.rehydrate(delegate, callback.getOwner(), callback.getThisObject())
                .call();
    }

    private void trimAccuracy() {
        if (accuracy == MetricEnums.MetricAccuracy.days) {
            return;
        }
        if (start.isBefore(LocalDateTime.now().minusDays(30))) {
            accuracy = MetricEnums.MetricAccuracy.days;
            return;
        }
        if (accuracy == MetricEnums.MetricAccuracy.hours) {
            return;
        }
        if (start.isBefore(LocalDateTime.now().minusDays(1))) {
            accuracy = MetricEnums.MetricAccuracy.hours;
        }
    }

    private Predicate<List<MetricVo>> timeFilter = metricVos -> {
        MetricVo anyOne = metricVos.get(0);
        if (start != null && anyOne.getCreateTime().isBefore(start)) {
            return false;
        }
        return end == null || !anyOne.getCreateTime().isAfter(end);
    };

    private Map<String, List<MetricVo>> filterAndMerge(Map<String, Context.MQLVar> metricData) {
        Map<String, List<MetricVo>> ret = new HashMap<>();
        metricData.forEach((k, v) -> {
            TreeMap<String, List<MetricVo>> filtered = filter(v.getData());
            List<MetricVo> merged = merge(filtered);
            ret.put(k, merged);
        });
        return ret;
    }

    private List<MetricVo> merge(TreeMap<String, List<MetricVo>> metricData) {
        Set<String> allMetricByTagId = metricData.values().stream()
                .flatMap((Function<List<MetricVo>, Stream<String>>) metricVos ->
                        metricVos.stream().map(MetricVo::toTagId))
                .collect(Collectors.toSet());

        return allMetricByTagId.stream().map(s -> {
            List<MetricVo> metricVos = metricData.values()
                    .stream().map(metricVoWithTags -> metricVoWithTags.stream()
                            .filter(metricVo -> metricVo.toTagId().equals(s))
                            .findAny()
                            .orElse(null))
                    .filter(Objects::nonNull).toList();
            if (metricVos.isEmpty()) {
                return null;
            }
            MetricVo first = metricVos.get(0);
            MetricVo ret = MetricVo.cloneMetricVo(first);
            switch (first.getType()) {
                case COUNTER -> ret.setValue(metricVos.stream().map(MetricVo::getValue).reduce(0d, Double::sum));
                case GAUGE -> ret.setValue(metricVos.get(metricVos.size() - 1).getValue());
                case TIMER -> {
                    String timerType = first.getTags().get(MetricEnums.TimeSubType.timer_type);
                    if (StringUtils.isBlank(timerType) || timerType.equals(MetricEnums.TimeSubType.MAX.metricKey)) {
                        // this is aggregated time-max
                        ret.setValue(metricVos.stream().map(MetricVo::getValue).reduce(0D, Double::max));
                    } else if (timerType.equals(MetricEnums.TimeSubType.TIME.metricKey) || timerType.equals(MetricEnums.TimeSubType.COUNT.metricKey)) {
                        ret.setValue(metricVos.stream().map(MetricVo::getValue).reduce(0D, Double::sum));
                    }
                }
            }
            return ret;
        }).filter(Objects::nonNull).toList();
    }

    private TreeMap<String, List<MetricVo>> filter(TreeMap<String, List<MetricVo>> data) {
        TreeMap<String, List<MetricVo>> filtered = new TreeMap<>();
        data.forEach((k, v) -> {
            if (timeFilter.test(v)) {
                filtered.put(k, v);
            }
        });
        return filtered;
    }

    private boolean isCronRunTime() {
        if (lastRun == null) {
            return true;
        }
        LocalDateTime next = cron.next(lastRun);
        if (next == null) {
            return false;
        }
        return !next.isAfter(LocalDateTime.now());
    }

}
