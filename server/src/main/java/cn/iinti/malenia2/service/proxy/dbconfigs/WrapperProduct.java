package cn.iinti.malenia2.service.proxy.dbconfigs;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.api.ip.resource.SessionParam;
import cn.iinti.malenia2.entity.Product;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.safethread.Looper;
import cn.iinti.malenia2.service.base.safethread.ValueCallback;
import cn.iinti.malenia2.service.base.trace.impl.SubscribeRecorders;
import cn.iinti.malenia2.service.proxy.ProxyServer;
import cn.iinti.malenia2.service.proxy.core.Session;
import cn.iinti.malenia2.service.proxy.core.outbound.ActiveProxyIp;
import cn.iinti.malenia2.service.proxy.utils.ConsistentHashUtil;
import cn.iinti.malenia2.service.proxy.utils.PortSpaceParser;
import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class WrapperProduct {
    public static final String METRIC_PREFIX = BuildConfig.appName + ".product.";
    /**
     * 本产品下的所有代理服务器
     */
    private TreeMap<Integer, ProxyServer> proxyServerTreeMap = Maps.newTreeMap();

    private TreeMap<Long, String> ipSourceKeyMap = new TreeMap<>();
    private List<String> ipSourceKeyList = Lists.newArrayList();
    /**
     * 路由缓存，当一个隧道访问过程使用过某个ip资源，那么系统优先尝试使用曾今的隧道，如此尽可能保证ip出口不变<br>
     * ps：需要做这个缓存更重要的原因是，malenia在运作过程会有failover，failover过程会有不可预期的隧道映射关系重置
     * 此时无法根据固定的规则进行mapping计算
     */
    private final Cache<Long, ActiveProxyIp> routeCache = CacheBuilder.newBuilder()
            .removalListener((RemovalListener<Long, ActiveProxyIp>) notification -> {
                Long sessionHash = notification.getKey();
                ActiveProxyIp activeProxyIp = notification.getValue();
                // auto decrease refCount of proxyIp
                if (sessionHash != null && activeProxyIp != null) {
                    activeProxyIp.refreshRefSessionHash(sessionHash, false);
                }
            })
            .expireAfterAccess(1, TimeUnit.MINUTES).build();

    @Delegate
    private Product product;

    @Getter
    private Looper productThread;

    public WrapperProduct reload(Product product) {
        this.product = product;
        if (productThread == null) {
            productThread = new Looper(product.getProductId()).startLoop();
        }
        productThread.post(() -> {
            TreeSet<Integer> mappingPort = PortSpaceParser.parsePortSpace(product.getMappingPortSpace());
            TreeMap<Integer, ProxyServer> newProxyServerMap = Maps.newTreeMap();
            for (Integer port : mappingPort) {
                ProxyServer inbound = proxyServerTreeMap.remove(port);
                if (inbound == null || !inbound.enable()) {
                    log.info("start proxy server for port: {}", port);
                    inbound = new ProxyServer(port, WrapperProduct.this);
                }
                newProxyServerMap.put(port, inbound);
            }
            TreeMap<Integer, ProxyServer> needRemove = proxyServerTreeMap;
            proxyServerTreeMap = newProxyServerMap;
            for (ProxyServer inbound : needRemove.values()) {
                inbound.destroy();
            }
        });

        return this;
    }

    private Map<String, Integer> floatRatio(Map<String, Integer> configRule) {
        configRule = new HashMap<>(configRule);

        boolean hasVerySmallConfig = configRule.values().stream().anyMatch(it -> it.equals(1) || it.equals(2));
        if (hasVerySmallConfig) {
            // expand ratio, because the float ratio component maybe decease config
            for (String ipSourceKey : Lists.newArrayList(configRule.keySet())) {
                configRule.put(ipSourceKey, configRule.get(ipSourceKey) * 3);
            }
        }

        // scale ratio config by health score
        for (String ipSourceKey : Lists.newArrayList(configRule.keySet())) {
            //IP池健康指数，正常值为100，最大值一般不超过150，小于100认为不健康，最低为0（代表此IP资源已经完全挂了）
            Double healthScore = DbConfigs.ipSourceHealthScore(ipSourceKey);
            if (healthScore == null) {
                continue;
            }
            Integer configuredRatio = configRule.get(ipSourceKey);
            configuredRatio = (int) (configuredRatio * healthScore / 100);
            if (configuredRatio <= 1) {
                configuredRatio = 1;
            }
            configRule.put(ipSourceKey, configuredRatio);
        }
        return configRule;
    }

    public void reloadIpSourceRatio(Map<String, Integer> ratio) {
        productThread.post(() -> {
            Map<String, Integer> configRule = Settings.enableFloatIpSourceRatio.value ?
                    floatRatio(ratio) : ratio;

            TreeMap<Long, String> newIpSources = new TreeMap<>();
            List<String> newIpSourceLists = Lists.newArrayList(new TreeSet<>(configRule.keySet()));

            for (String ipSourceKey : configRule.keySet()) {
                int ratio1 = configRule.get(ipSourceKey);
                if (ratio1 == 0) {
                    continue;
                }
                for (int i = 1; i <= ratio1; i++) {
                    long murHash = ConsistentHashUtil.murHash(ipSourceKey + "_##_" + i);
                    newIpSources.put(murHash, ipSourceKey);
                }
            }
            ipSourceKeyMap = newIpSources;
            ipSourceKeyList = newIpSourceLists;
        });
    }

    private String fetchPreferIpSource(Session session, long sessionHash) {
        String ipSourceKey = SessionParam.OUTBOUND_PREFER_IP_SOURCE.get(session.getSessionParam());
        if (ipSourceKey == null) {
            ipSourceKey = ConsistentHashUtil.fetchConsistentRing(ipSourceKeyMap, sessionHash);
        }
        return ipSourceKey;
    }


    public void destroy() {
        productThread.post(() -> {
            proxyServerTreeMap.values().forEach(ProxyServer::destroy);
            productThread.close();
        });
    }

    public void connectToOutbound(Session session, long sessionHash, String tag,
                                  ActiveProxyIp.ActivityProxyIpBindObserver observer, ValueCallback<Channel> callback) {
        SubscribeRecorders.SubscribeRecorder recorder = session.getRecorder();
        allocateIpSource(sessionHash, tag, session, ipSourceValue -> {
            if (!ipSourceValue.isSuccess()) {
                recorder.recordEvent(() -> tag + "allocate IpSource failed", ipSourceValue.e);
                ValueCallback.failed(callback, ipSourceValue.e);
                return;
            }

            WrapperIpSource ipSource = ipSourceValue.v;
            // 拿到IP源，此时ip源是根据分流比例控制的
            recorder.recordMosaicMsg(() -> tag + "allocate IpSource success: " + ipSource.getSourceKey());
            // 在ip源上分配代理
            ipSource.getIpPool().allocateIp(sessionHash, session.getSessionParam(), session.getRecorder(), ipWrapper -> {
                if (!ipWrapper.isSuccess()) {
                    recorder.recordEvent(() -> tag + "allocate IP failed");
                    ValueCallback.failed(callback, ipWrapper.e);
                    return;
                }

                ActiveProxyIp activeProxyIp = ipWrapper.v;
                recorder.recordMosaicMsg(() -> tag + "allocate IP success:" + JSONObject.toJSONString(activeProxyIp.getDownloadProxyIp()) + " begin to borrow connection");
                activeProxyIp.borrowConnect(recorder, tag, observer, callback);
            });
        });
    }

    public void allocateIpSource(long sessionHash, String tag, Session session, ValueCallback<WrapperIpSource> valueCallback) {
        productThread.post(() -> {
            // 如果用户有指定了代理ip源，那么使用特定的代理ip
            String ipSourceKey = fetchPreferIpSource(session, sessionHash);
            if (ipSourceKey == null) {
                // not happen
                valueCallback.onReceiveValue(ValueCallback.Value.failed("no ipSources mapping for product:" + getProductId()));
                return;
            }
            session.getRecorder().recordMosaicMsg(() -> tag + "product mapper route to ipSource: " + ipSourceKey);
            DbConfigs.allocateIpSource(session.getRecorder(), tag, ipSourceKey, ipSourceKeyList, valueCallback);
        });
    }


    public void fetchCachedSession(Session session, ValueCallback<ActiveProxyIp> callback) {
        productThread.post(() -> {
            ActiveProxyIp sessionIp = routeCache.getIfPresent(session.getSessionHash());
            if (sessionIp == null) {
                ValueCallback.failed(callback, "not exist");
                return;
            }

            if (sessionIp.getActiveStatus() != ActiveProxyIp.ActiveStatus.DESTROY) {
                ValueCallback.success(callback, sessionIp);
                return;
            }
            routeCache.invalidate(session.getSessionHash());
            ValueCallback.failed(callback, "not exist");
        });

    }

    public void markSessionUse(Session session, ActiveProxyIp activeProxyIp) {
        activeProxyIp.increaseUse();
        if (session.getWrapperOrder().getRandomTurning()) {
            return;
        }
        session.getRecorder().recordEvent(() -> "add sessionId route mapping ");
        activeProxyIp.refreshRefSessionHash(session.getSessionHash(), true);
        productThread.post(() -> routeCache.put(session.getSessionHash(), activeProxyIp));
    }

    public Collection<Integer> usedPorts() {
        return proxyServerTreeMap.values().stream().map(
                server -> server.port
        ).collect(Collectors.toList());
    }
}
