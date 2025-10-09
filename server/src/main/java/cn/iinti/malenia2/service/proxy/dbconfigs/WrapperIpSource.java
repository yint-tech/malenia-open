package cn.iinti.malenia2.service.proxy.dbconfigs;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.api.ip.resource.*;
import cn.iinti.malenia2.api.resource.GroovyIpResourceHandler;
import cn.iinti.malenia2.entity.IpSource;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.base.safethread.Looper;
import cn.iinti.malenia2.service.base.safethread.NumberView;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.base.trace.impl.SubscribeRecorders;
import cn.iinti.malenia2.service.proxy.core.mitm.api.ApiImpl;
import cn.iinti.malenia2.service.proxy.core.outbound.IpPool;
import cn.iinti.malenia2.service.proxy.core.outbound.LastErrorsSlot;
import cn.iinti.malenia2.service.proxy.core.outbound.downloader.IpDownloader;
import cn.iinti.malenia2.service.proxy.core.outbound.handshark.Protocol;
import cn.iinti.malenia2.utils.Md5Utils;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WrapperIpSource {

    public static final String METRIC_PREFIX = BuildConfig.appName + ".ipSource.";

    /**
     * 数据库的ip池配置
     */
    @Delegate
    private IpSource ipSource;


    /**
     * 底层组件，ip池功能太复杂，抽象多个模块分别处理
     */
    @Delegate
    private final IpSourceComponent component;

    /**
     * 构建上游代理ip鉴权账户表达式，当上游代理源提供根据鉴权账户的控制指令能力时，本模块负责处理控制指令的表达<br>
     * 即扩展账户名的语义，使得他有传递控制参数的功能，如设定出口城市、出口国家、控制超时时间、控制经纬度最近搜索等<br>
     * 此功能在海外代理供应商非常常见，如luminati
     */
    private IpAuthBuilder ipAuthBuilder;

    @Delegate
    private final LastErrorsSlot lastErrorsSlot = new LastErrorsSlot();


    /**
     * 本IP池支持的代理协议：http/https/socks5
     */
    @Getter
    private List<Protocol> supportProtocolList;

    public WrapperIpSource(IpSource ipSource) {
        this.ipSource = ipSource;
        this.component = new IpSourceComponent(ipSource);
        this.ipAuthBuilder = createUpstreamUserNameBuilder();
        this.supportProtocolList = Collections.unmodifiableList(parseSupportProtocol(ipSource.getSupportProtocol()));
    }

    public void reload(IpSource ipSource) {
        this.ipSource = ipSource;
        this.component.reloadIpResourceHandler(ipSource.getLoadResourceHandler());
        this.ipAuthBuilder = createUpstreamUserNameBuilder();
        this.supportProtocolList = Collections.unmodifiableList(parseSupportProtocol(ipSource.getSupportProtocol()));
    }

    public IpAuthBuilder.AuthUser buildAuthUser(Map<String, String> sessionParam, String defaultPassword) {
        IpAuthBuilder.AuthUser.AuthUserBuilder authUser = IpAuthBuilder.AuthUser.builder();
        ipAuthBuilder.buildAuthUser(sessionParam, authUser);
        if (StringUtils.isBlank(authUser.get_Password())) {
            authUser.password(defaultPassword);
        }
        if(StringUtils.isBlank(authUser.get_UserName())){
            authUser.userName(SessionParam.OUTBOUND_USER.get(sessionParam));
        }
        return authUser.build();
    }

    private IpAuthBuilder createUpstreamUserNameBuilder() {
        return new IpAuthBuilder() {
            @Override
            public void buildAuthUser(Map<String, String> sessionParam, AuthUser.AuthUserBuilder authUser) {
                component.resourceHandler.buildAuthUser(sessionParam, authUser);
                if (StringUtils.isBlank(authUser.get_UserName())) {
                    authUser.userName(getUpUserName());
                }
                if (StringUtils.isBlank(authUser.get_Password())) {
                    authUser.password(getUpUserPassword());
                }
            }
        };
    }

    private static final Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();

    public static List<Protocol> parseSupportProtocol(String config) {
        List<Protocol> protocols = Lists.newArrayList();
        for (String protocolStr : splitter.split(config)) {
            Protocol protocol = Protocol.get(protocolStr);
            if (protocol == null) {
                throw new IllegalArgumentException("error support protocol config:" + protocolStr);
            }
            protocols.add(protocol);
        }
        protocols.sort((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));
        return protocols;
    }

    public boolean poolEmpty() {
        return getIpPool().poolEmpty();
    }

    public double healthScore() {
        return getIpPool().healthScore();
    }

    @Getter
    private class IpSourceComponent {
        /**
         * 代理IP池，真正存储IP资源，以及缓存的可用tcp连接
         */
        private final IpPool ipPool;

        /**
         * 从代理源加载ip资源的下载器
         */
        private final IpDownloader ipDownloader;

        /**
         * 绑定ip池的线程，使用单线程模型完成资源分发
         */
        private final Looper looper;

        private final Recorder recorder;
        /**
         * 代理资源文本解析器
         */
        private IpResourceHandler resourceHandler;

        public IpSourceComponent(IpSource ipSource) {
            String sourceKey = ipSource.getSourceKey();
            String beanId = "IpSource-" + sourceKey;
            looper = new Looper(beanId).startLoop();
            recorder = SubscribeRecorders.IP_SOURCE.acquireRecorder(beanId, Environment.isLocalDebug, sourceKey);
            ipPool = new IpPool(WrapperIpSource.this, looper, recorder, sourceKey);
            ipDownloader = new IpDownloader(WrapperIpSource.this, recorder, sourceKey);
            // 先加载resourceHandler，再启动任务，因为加载过程可能需要编译代码，此时比较耗时
            reloadIpResourceHandler(ipSource.getLoadResourceHandler());

            // 这里会启动下载任务，所以最后执行
            setupSchedulerTask();
        }

        private void setupSchedulerTask() {
            looper.scheduleWithRate(WrapperIpSource.this::scheduleIpDownload, new NumberView(() -> getReloadInterval() * 1000));
            looper.scheduleWithRate(WrapperIpSource.this::scheduleMakeConnCache, new NumberView(() -> getMakeConnInterval() * 1000));

            looper.postDelay(WrapperIpSource.this::scheduleIpDownload, 500);
        }

        private String handlerMd5;

        private void reloadIpResourceHandler(String handlerScriptCode) {
            handlerScriptCode = StringUtils.defaultIfBlank(handlerScriptCode, "IpPortPlain");

            String md5 = Md5Utils.md5Hex(handlerScriptCode);
            if (md5.equals(handlerMd5)) {
                return;
            }
            IpResourceHandler delegate = WrapperIpSource.resolveIpResourceHandler(handlerScriptCode);
            this.resourceHandler = new ExceptionCatchIpResourceHandler(delegate);
            handlerMd5 = md5;
        }

        private class ExceptionCatchIpResourceHandler implements IpResourceHandler {
            private final IpResourceHandler delegate;

            public ExceptionCatchIpResourceHandler(IpResourceHandler delegate) {
                this.delegate = delegate;
            }

            @Override
            public List<ProxyIp> parse(String responseText) {
                try {
                    return delegate.parse(responseText);
                } catch (Exception e) {
                    Monitor.counter(METRIC_PREFIX + ".ipPool.parseFailed", "sourceKey", getSourceKey()).increment();
                    recorder.recordEvent(() -> "parse response ip content failed", e);
                    return Collections.emptyList();
                }
            }

            @Override
            public void onProxyIpDrop(ProxyIp proxyIp, CountStatus countStatus, DropReason dropReason) {
                try {
                    delegate.onProxyIpDrop(proxyIp, countStatus, dropReason);
                } catch (Exception e) {
                    Monitor.counter(METRIC_PREFIX + ".ipPool.proxyIpDropNotifyFailed", "sourceKey", getSourceKey()).increment();
                    recorder.recordEvent(() -> "onProxyIpDrop notify failed", e);
                }
            }

            @Override
            public void buildAuthUser(Map<String, String> sessionParam, AuthUser.AuthUserBuilder builder) {
                try {
                    delegate.buildAuthUser(sessionParam, builder);
                } catch (Exception e) {
                    Monitor.counter(METRIC_PREFIX + ".ipPool.buildAuthUserFailed", "sourceKey", getSourceKey()).increment();
                    recorder.recordEvent(() -> "buildAuthUserFailed failed", e);
                }
            }
        }
    }


    public void recordComposedEvent(Recorder userTraceRecorder, Recorder.MessageGetter messageGetter) {
        userTraceRecorder.recordEvent(messageGetter);
        getRecorder().recordEvent(messageGetter);
    }

    public void recordComposedMosaicEvent(Recorder userTraceRecorder, Recorder.MessageGetter messageGetter) {
        userTraceRecorder.recordMosaicMsgIfSubscribeRecorder(messageGetter);
        getRecorder().recordEvent(messageGetter);
    }


    private void scheduleIpDownload() {
        getLooper().checkLooper();
        getRecorder().recordEvent("begin download ip");
        getIpDownloader().downloadIp();
    }

    public void scheduleMakeConnCache() {
        getIpPool().makeCache();
    }

    public void destroy() {
        Looper looper = getLooper();
        looper.execute(() -> {
            getIpPool().destroy();
            looper.postDelay(looper::close, 30_000);
        });
    }

    private static boolean hasScriptCode(String code) {
        return StringUtils.isNotBlank(code)
                && !StringUtils.equalsAnyIgnoreCase(code, "IpPortPlain", "PortSpace");
    }

    public static IpResourceHandler resolveIpResourceHandler(String groovyScriptSource) {
        return hasScriptCode(groovyScriptSource) ? GroovyIpResourceHandler.compileScript(groovyScriptSource,
                new ApiImpl("__malenia_system")) : SmartParser.instance;
    }

}
