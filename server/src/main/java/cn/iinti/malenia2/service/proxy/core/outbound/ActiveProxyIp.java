package cn.iinti.malenia2.service.proxy.core.outbound;

import ch.hsr.geohash.GeoHash;
import cn.iinti.malenia2.api.ip.resource.*;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.base.safethread.Looper;
import cn.iinti.malenia2.service.base.safethread.ValueCallback;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.outbound.downloader.DownloadProxyIp;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperIpSource;
import cn.iinti.malenia2.service.proxy.utils.ConsistentHashUtil;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import cn.iinti.malenia2.utils.Md5Utils;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import io.micrometer.core.instrument.Tags;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ActiveProxyIp {

    @Getter
    private final DownloadProxyIp downloadProxyIp;

    private final Looper workThread;
    private final Recorder recorder;
    @Getter
    private final IpPool ipPool;
    @Getter
    private String geoHashCode;

    @Getter
    private String adminCode;

    @Getter
    private final long murHash;
    @Getter
    private final long seq;

    @Getter
    private volatile ActiveStatus activeStatus;


    private final Set<Channel> usedChannels = ConcurrentHashMap.newKeySet();

    private final CacheHandle cachedHandle = new CacheHandle();

    private final Set<Long> usedHash = new HashSet<>();

    @Getter
    private final CountStatus countStatus = new CountStatus();

    private Runnable destroyFun;

    public ActiveProxyIp(IpPool ipPool, DownloadProxyIp downloadProxyIp, long seq) {
        this.ipPool = ipPool;
        this.seq = seq;
        this.downloadProxyIp = downloadProxyIp;
        this.workThread = ipPool.getWrapperIpSource().getLooper();
        this.recorder = ipPool.getWrapperIpSource().getRecorder();
        this.activeStatus = ActiveStatus.ONLINE;

        this.murHash = ConsistentHashUtil.murHash(downloadProxyIp.getResourceId());
        resolveGeo();
    }

    public static String transformGeoHashCodeFromExp(String exp, long hash) {
        List<String> strings = SessionParam.slashSplitter.splitToList(exp);
        if (strings.size() < 2) {
            return null;
        }
        double lng = NumberUtils.toDouble(strings.get(0), 1000);
        double lat = NumberUtils.toDouble(strings.get(1), 1000);
        try {
            return GeoHash.withCharacterPrecision(lat, lng, 8).longValue()
                    + "" + hash;
        } catch (Exception e) {
            return null;
        }
    }

    private void resolveGeo() {
        CityResponse cityResponse = downloadProxyIp.getCityResponse();
        if (cityResponse == null) {
            return;
        }
        Location location = cityResponse.getLocation();
        Double longitude = location.getLongitude();
        Double latitude = location.getLatitude();
        if (longitude != null && latitude != null) {
            geoHashCode = GeoHash.withCharacterPrecision(latitude, longitude, 8).longValue()
                    + "" + ThreadLocalRandom.current().nextInt(99999);
        }

        // 国家编码： https://en.wikipedia.org/wiki/ISO_3166-1
        // 如 中国： CN，美国：US，越南：VN
        String isoCode = cityResponse.getCountry().getIsoCode();
        if (StringUtils.isNotBlank(isoCode)) {
            StringBuilder adminCodeBuilder = new StringBuilder(isoCode);

            String cityName = fetchAdminName(cityResponse.getCity().getNames());
            if (cityName != null) {
                adminCodeBuilder
                        .append('_')
                        .append(Md5Utils.toHexString(cityName.getBytes(StandardCharsets.UTF_8)))
                        // 在最后附加随机数，因为同一个城市需要支持多个ip，需要避免他们被覆盖
                        // 我们最在统一个城市支持10w
                        .append(ThreadLocalRandom.current().nextInt(99999))
                ;
            }
            adminCode = adminCodeBuilder.toString();
        }
    }

    private String fetchAdminName(Map<String, String> names) {
        if (names.isEmpty()) {
            return null;
        }
        if (names.size() == 1) {
            return names.values().iterator().next();
        }
        if (names.containsKey("zh")) {
            return names.get("zh");
        }
        if (names.containsKey("en")) {
            return names.get("en");
        }
        return null;
    }

    public void destroy(DropReason dropReason) {
        if (activeStatus == ActiveStatus.DESTROY) {
            return;
        }
        workThread.execute(() -> {
            if (activeStatus == ActiveStatus.DESTROY) {
                return;
            }
            if (activeStatus == ActiveStatus.ONLINE) {
                activeStatus = ActiveStatus.OFFLINE;
            }

            destroyFun = () -> {
                workThread.checkLooper();
                if (activeStatus == ActiveStatus.DESTROY) {
                    return;
                }
                activeStatus = ActiveStatus.DESTROY;
                destroyFun = null;
                IpResourceHandler resourceHandler = getIpPool().getWrapperIpSource().getResourceHandler();
                cachedHandle.destroy();
                resourceHandler.onProxyIpDrop(downloadProxyIp.getProxyIp(), countStatus, dropReason);
            };
            if (usedHash.isEmpty() || dropReason == DropReason.IP_SERVER_UNAVAILABLE) {
                // 如果当前没有用户占用本ip，则立即销毁
                destroyFun.run();
                return;
            }
            // 否则给一个60s的延时时间，给业务继续使用
            // 请注意整个60s的延时不是确定的，如果业务提前判断全部离开本ip，则清理动作可以提前
            workThread.postDelay(destroyFun, 60_000);
        });


    }


    public void borrowConnect(Recorder recorder, String tag, ActivityProxyIpBindObserver observer, ValueCallback<Channel> valueCallback) {
        observer.onBind(this);
        workThread.execute(() -> {
            cachedHandle.onConnBorrowed();
            boolean borrowHintCache = false;
            try {
                // 尝试使用缓存的ip资源
                while (true) {
                    Channel one = cachedHandle.cachedChannels.poll();
                    if (one == null) {
                        break;
                    }
                    if (one.isActive()) {
                        recorder.recordEvent(() -> tag + "conn cache pool hinted");
                        borrowHintCache = true;
                        ValueCallback.success(valueCallback, one);
                        return;
                    }
                }
                recorder.recordEvent(() -> tag + "begin to create connection immediately");

                createUpstreamConnection(valueCallback, recorder);
            } finally {
                Monitor.counter(WrapperIpSource.METRIC_PREFIX + "ipPool.connCacheHint",
                        Tags.concat(ipPool.getTags(), "hinted", String.valueOf(borrowHintCache))
                ).increment();
            }
        });

    }

    public boolean isIdle() {
        return usedChannels.isEmpty();
    }

    private static final AttributeKey<ActiveProxyIp> ACTIVITY_PROXY_IP_KEY = AttributeKey.newInstance("ACTIVITY_PROXY_IP");

    private void createUpstreamConnection(ValueCallback<Channel> valueCallback, Recorder userRecorder) {
        OutboundOperator.connectToServer(downloadProxyIp.getProxyHost(), downloadProxyIp.getProxyPort(), value -> {
            Monitor.counter(WrapperIpSource.METRIC_PREFIX + "ipPool.connectToIp",
                    Tags.concat(ipPool.getTags(), "success", String.valueOf(value.isSuccess()))
            ).increment();

            if (!value.isSuccess()) {
                // 这里失败我们我们不再执行立即替换出口ip的逻辑，这是因为在并发极高的情况下
                // 失败可能是我们自己的网络不通畅导致的，我们不能以链接失败就判定ip存在问题
                ipPool.onCreateConnectionFailed(ActiveProxyIp.this, value.e, userRecorder);
                ValueCallback.failed(valueCallback, value.e);
                return;
            }

            // setup meta info
            Channel channel = value.v;
            channel.attr(ACTIVITY_PROXY_IP_KEY).set(this);
            channel.closeFuture().addListener(it -> usedChannels.remove(channel));
            cachedHandle.scheduleCleanIdleCache(channel, ipPool.getWrapperIpSource().getConnIdleSeconds());

            ValueCallback.success(valueCallback, channel);
        });
    }

    public static void restoreCache(Channel channel) {
        ActiveProxyIp activeProxyIp = getBinding(channel);
        if (activeProxyIp == null) {
            channel.close();
            return;
        }
        activeProxyIp.cachedHandle.restoreCache(channel);
    }

    public static void offlineBindingProxy(Channel channel, IpPool.OfflineLevel level, Recorder userRecorder) {
        ActiveProxyIp activeProxyIp = getBinding(channel);
        if (activeProxyIp == null) {
            return;
        }
        activeProxyIp.getIpPool().offlineProxy(activeProxyIp, level, userRecorder);
    }

    public static ActiveProxyIp getBinding(Channel channel) {
        return channel.attr(ACTIVITY_PROXY_IP_KEY).get();
    }

    public void makeCache() {
        if (activeStatus != ActiveStatus.ONLINE) {
            return;
        }
        cachedHandle.doCreateCacheTask();
    }

    private class CacheHandle {
        private final LinkedList<Channel> cachedChannels = Lists.newLinkedList();
        /**
         * 用户请求的平均时间间隔，代表这个ip资源处理请求的qps，但是我们使用一个高效计算方案评估这个指标<br>
         * 这个值将会约等于最近10次请求时间间隔的平均数
         */
        private double avgInterval = 1000;

        private long lastRequestConnection = 0;

        private long lastCreateCacheConnection = 0;

        private void destroy() {
            NettyUtil.closeAll(cachedChannels);
            cachedChannels.clear();
        }

        public void restoreCache(Channel channel) {
            workThread.execute(() -> {
                if (activeStatus == ActiveStatus.DESTROY) {
                    channel.close();
                    return;
                }
                cachedChannels.addFirst(channel);
            });
        }

        public void onConnBorrowed() {
            long now = System.currentTimeMillis();
            if (lastRequestConnection == 0L) {
                lastRequestConnection = now;
                return;
            }

            long thisInterval = now - lastRequestConnection;
            lastRequestConnection = now;
            avgInterval = (thisInterval * 9 + avgInterval) / 10;
        }

        void scheduleCleanIdleCache(Channel cacheChannel, Integer idleSeconds) {
            // 为了避免gc hold，所有延时任务里面不允许直接访问channel对象，而使用WeakReference
            WeakReference<Channel> channelRef = new WeakReference<>(cacheChannel);
            workThread.postDelay(() -> {
                Channel gcChannel = channelRef.get();
                if (gcChannel == null || !gcChannel.isActive()) {
                    return;
                }
                for (Channel ch : cachedChannels) {
                    if (ch.equals(gcChannel)) {
                        gcChannel.close();
                        return;
                    }
                }
            }, idleSeconds * 1000);
        }


        public void doCreateCacheTask() {
            if (cachedChannels.size() > 3) {
                recorder.recordEvent(() -> "skip make conn cache because of cache overflow:" + cachedChannels.size());
                //理论上真实流量一半的速率，cachedChannels.size()应该为0或者1
                return;
            }

            long now = System.currentTimeMillis();
            if (lastCreateCacheConnection != 0 && (now - lastRequestConnection) < avgInterval / 2) {
                // 流量为真实速率的一半
                return;
            }
            lastCreateCacheConnection = now;

            recorder.recordEvent(() -> "fire conn cache make task");
            createUpstreamConnection(value -> {
                if (value.isSuccess()) {
                    workThread.execute(() -> cachedChannels.addFirst(value.v));
                }
            }, Recorder.nop);
        }

    }

    public boolean skipAuth(Map<String, String> sessionParam) {
        return buildAuthenticationInfo(sessionParam, Recorder.nop) == null;
    }

    @Nullable
    public IpAuthBuilder.AuthUser buildAuthenticationInfo(Map<String, String> sessionParam, Recorder recorder) {
        SessionParam.OUTBOUND_USER.set(sessionParam, StringUtils.defaultString(
                downloadProxyIp.getUserName(),
                getIpPool().getWrapperIpSource().getUpUserName())
        );
        // 请注意，这里的目的是支持 Luminati，这家供应商通过每次在用户里面增加扩展信息的方式实现高级特性控制
        IpAuthBuilder.AuthUser authUser = getIpPool().getWrapperIpSource().buildAuthUser(sessionParam, downloadProxyIp.getPassword());
        recorder.recordMosaicMsgIfSubscribeRecorder(() -> "upstream user: " + authUser);
        if (!authUser.hasAuth()) {
            return null;
        }
        return authUser;
    }

    public String buildHttpAuthenticationInfo(Map<String, String> sessionParam, Recorder recorder) {
        IpAuthBuilder.AuthUser authenticationInfo = buildAuthenticationInfo(sessionParam, recorder);
        if (authenticationInfo == null) {
            return null;
        }

        String authorizationBody = authenticationInfo.getUserName() + ":" + authenticationInfo.getPassword();
        return "Basic " + BaseEncoding.base64().encode(authorizationBody.getBytes(StandardCharsets.UTF_8));
    }

    public void refreshRefSessionHash(long sessionHash, boolean add) {
        workThread.execute(() -> {
            if (add) {
                usedHash.add(sessionHash);
                return;
            }
            usedHash.remove(sessionHash);
            if (usedHash.isEmpty() && destroyFun != null) {
                destroyFun.run();
            }
        });
    }

    public void increaseUse() {
        countStatus.setTotalCount(countStatus.getTotalCount() + 1);
        if (countStatus.getFistActive() == 0) {
            countStatus.setFistActive(System.currentTimeMillis());
        }
    }

    public int cacheConnSize() {
        return cachedHandle.cachedChannels.size();
    }

    public enum ActiveStatus {
        ONLINE,
        OFFLINE,
        DESTROY
    }

    public interface ActivityProxyIpBindObserver {
        void onBind(ActiveProxyIp activeProxyIp);
    }


}
