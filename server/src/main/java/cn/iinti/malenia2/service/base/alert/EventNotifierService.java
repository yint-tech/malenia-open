package cn.iinti.malenia2.service.base.alert;

import cn.iinti.malenia2.service.base.alert.events.DiskPoorEvent;
import cn.iinti.malenia2.service.base.alert.events.IpSourceHealthScoreLowEvent;
import cn.iinti.malenia2.service.base.alert.events.SensitiveOperationEvent;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.metric.MetricService;
import cn.iinti.malenia2.service.base.safethread.Looper;
import cn.iinti.malenia2.service.proxy.dbconfigs.DbConfigs;
import cn.iinti.malenia2.utils.ServerIdentifier;
import groovy.lang.Closure;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class EventNotifierService implements CommandLineRunner {
    private final Looper eventNotifierThread = new Looper("eventNotifier").startLoop();

    @Resource
    private MetricService metricService;

    private Map<String, MetricMonitorHandle> metricNotifyRecord = new LinkedHashMap<>();

    public void scheduleIpSourceHealthLowEvent() {
        withScriptExtension((eventScript -> {
            DbConfigs.ipSourceList().forEach(ipSource -> {
                Double score = DbConfigs.ipSourceHealthScore(ipSource);
                log.info("ipSourceHealthScore,source:{} {}", ipSource, score);
                if (score == null || score > 50) {
                    return;
                }
                log.info("notify ipSourceHealthScore event...");
                IpSourceHealthScoreLowEvent event = new IpSourceHealthScoreLowEvent(ServerIdentifier.id(), ipSource, score);
                callExtensions(eventScript.ipSourceHealthScoreLowEventList, event);
            });
        }));
    }

    public void notifySensitiveOperation(String user, String api, String params) {
        withScriptExtension((eventScript -> {
            SensitiveOperationEvent event = new SensitiveOperationEvent(user, api, params);
            callExtensions(eventScript.sensitiveEventList, event);
        }));
    }


    private void scheduleDiskSpacePoorEvent() {
        File root = Settings.Storage.root;
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        if (freeSpace * 100 / totalSpace > 25) {
            return;
        }
        withScriptExtension(eventScript -> {
            DiskPoorEvent diskPoorEvent = new DiskPoorEvent(totalSpace, freeSpace, ServerIdentifier.id());
            callExtensions(eventScript.diskPoorEventList, diskPoorEvent);
        });
    }

    public void scheduleMetricEvent(boolean force) {
        withScriptExtension(eventScript -> {
            Map<String, MetricMonitorHandle> newRecords = new HashMap<>();
            eventScript.metricMonitorConfigList.forEach(config -> {
                MetricMonitorHandle metricMonitorHandle =
                        metricNotifyRecord.computeIfAbsent(config.getId(), (k) -> new MetricMonitorHandle());

                config.fillMeta(metricMonitorHandle);
                try {
                    metricMonitorHandle.evaluate(metricService, force);
                } catch (Exception e) {
                    log.warn("metric evaluate error", e);
                }
                newRecords.put(config.getId(), metricMonitorHandle);
            });
            metricNotifyRecord = newRecords;
        });
    }


    private void callExtensions(List<Closure<?>> eventList, Object delegate) {
        for (Closure<?> closure : eventList) {
            try {
                closure.rehydrate(delegate, closure.getOwner(), closure.getThisObject())
                        .call();
            } catch (Exception e) {
                log.info("call extension failed", e);
            }
        }
    }

    private void withScriptExtension(Consumer<EventScript> fun) {
        EventScript eventScript = Settings.eventNotifyScript.value;
        if (eventScript == null) {
            return;
        }
        eventNotifierThread.post(() -> fun.accept(eventScript));
    }

    @Override
    public void run(String... args) {
        eventNotifierThread.scheduleWithRate(this::scheduleDiskSpacePoorEvent, 30 * 60 * 1000);
        eventNotifierThread.scheduleWithRate(() -> scheduleMetricEvent(false), 10 * 60 * 1000);
        eventNotifierThread.scheduleWithRate(this::scheduleIpSourceHealthLowEvent, 25 * 60 * 1000);
    }
}
