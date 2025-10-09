package cn.iinti.malenia2.service.backend;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.base.safethread.Looper;
import cn.iinti.malenia2.service.base.safethread.ValueCallback;
import cn.iinti.malenia2.service.proxy.core.Session;
import cn.iinti.malenia2.utils.Md5Utils;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AccessRecordCollector {
    private static final Looper recordLooper = new Looper("AccessRecordCollector").startLoop();

    public static void recordAccess(Session session) {
        recordLooper.execute(() -> {
            Monitor.counter(BuildConfig.appName + ".access.session",
                    "user", session.getWrapperUser().getUserName(),
                    "protocol", session.getInboundProtocol().name(),
                    "product", session.getWrapperProduct().getProductId()
            ).increment();

            if (!Settings.enableAccessRecord.value) {
                return;
            }
            String hour = LocalDateTime.now().format(hourPattern);
            HourGroup hourGroup = sData.computeIfAbsent(hour, HourGroup::new);
            hourGroup.recordAccess(session);

            if (sData.keySet().size() > 2) {
                // 删除历史的记录，我们按小时翻滚
                sData.remove(sData.firstKey());
            }
        });
    }


    public static void fetchAccessRecords(LocalDateTime localDateTime, ValueCallback<Collection<AccessRecordData>> valueCallback) {
        String hour = localDateTime.format(hourPattern);
        recordLooper.post(() -> {
            HourGroup hourGroup = sData.get(hour);
            if (hourGroup == null) {
                ValueCallback.success(valueCallback, Collections.emptyList());
                return;
            }
            Collection<AccessRecordData> values = hourGroup.holderMap.values();
            ValueCallback.success(valueCallback, values);
        });
    }


    private static final DateTimeFormatter hourPattern = DateTimeFormatter.ofPattern("yyyyMMdd-HH");

    private static final TreeMap<String, HourGroup> sData = new TreeMap<>();

    private static class HourGroup {
        private final String hourString;
        private final Map<String, AccessRecordData> holderMap;

        public HourGroup(String hourString) {
            this.hourString = hourString;
            holderMap = new HashMap<>();
        }

        private void recordAccess(Session session) {
            recordLooper.checkLooper();
            String user = session.getWrapperUser().getUserName();
            String targetHost = session.getConnectTarget().getIpPort();
            String md5 = makeAccessRecordMd5(user, targetHost, hourString);

            AccessRecordData accessRecordData = holderMap.get(md5);
            if (accessRecordData == null) {
                accessRecordData = new AccessRecordData(md5, user, targetHost, hourString);
                holderMap.put(md5, accessRecordData);
            }
            accessRecordData.count.incrementAndGet();
        }
    }

    public static class AccessRecordData {
        public AccessRecordData(String md5, String user, String targetHost, String hour) {
            this.md5 = md5;
            this.user = user;
            this.targetHost = targetHost;
            this.hour = hour;
        }

        @Getter
        private final String md5;
        @Getter
        private final String user;
        @Getter
        private final String targetHost;
        @Getter
        private final String hour;
        private final AtomicInteger count = new AtomicInteger(0);

        public int getCount() {
            return count.get();
        }
    }


    public static String makeAccessRecordMd5(String user, String targetHost, String hour) {
        return Md5Utils.md5Hex(user + "---" + targetHost + "---" + hour);
    }
}
