package cn.iinti.malenia2.service.proxy.core.outbound.downloader;

import cn.iinti.malenia2.api.ip.resource.*;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.base.safethread.Looper;
import cn.iinti.malenia2.service.base.safethread.ValueCallback;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.base.trace.impl.SubscribeRecorders;
import cn.iinti.malenia2.service.proxy.client.AsyncHttpInvoker;
import cn.iinti.malenia2.service.proxy.client.ProxyInfo;
import cn.iinti.malenia2.service.proxy.core.outbound.handshark.Protocol;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperIpSource;
import cn.iinti.malenia2.utils.IpUtil;
import cn.iinti.malenia2.utils.ResourceUtil;
import cn.iinti.malenia2.utils.ServerIdentifier;
import com.google.common.collect.Maps;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import io.micrometer.core.instrument.Tags;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IpDownloader {
    private static final DatabaseReader ipGeoDb = openGeoDb();
    private final WrapperIpSource wrapperIpSource;
    private final Looper workThread;
    private volatile boolean isDownloading = false;
    private final Recorder recorder;

    private final Tags tags;


    public IpDownloader(WrapperIpSource wrapperIpSource, Recorder recorder, String sourceKey) {
        this.wrapperIpSource = wrapperIpSource;
        // downloader里面全是异步，但是可能会调用dns服务，并且可能存在对无效的代理域名进行dns解析，
        // 这可能存在些许耗时，所以这里我新建一个线程来处理
        this.workThread = new Looper("Downloader-" + sourceKey).startLoop();
        this.recorder = recorder;
        tags = Tags.of("sourceKey", wrapperIpSource.getSourceKey());
    }

    private static final String dbName = "GeoLite2-City.mmdb";

    @SneakyThrows
    private static DatabaseReader openGeoDb() {
        File dbFile = new File(Settings.Storage.root, dbName);

        if (!dbFile.exists()) {
            // 压缩一下让代码文件小一点儿 60M -> 30M
            try (ZipInputStream zipInputStream = new ZipInputStream(ResourceUtil.openResource(dbName + ".zip.bin"))) {
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry == null || !dbName.equals(nextEntry.getName())) {
                    throw new IllegalStateException("error geo db zip resource file: " + dbName);
                }
                byte[] bytes = IOUtils.toByteArray(zipInputStream);
                FileUtils.writeByteArrayToFile(dbFile, bytes);
            }
        }

        return new DatabaseReader
                .Builder(dbFile)
                .withCache(new CHMCache())
                .build();
    }

    public void downloadIp() {
        workThread.execute(() -> {
            String loadUrl = wrapperIpSource.getLoadUrl();
            if (!isHTTPLink(loadUrl)) {
                onDownloadResponse(loadUrl);
                return;
            }

            if (isDownloading) {
                recorder.recordEvent(() -> "download task is running");
                wrapperIpSource.appendErrorRecord("download task is running");
                return;
            }
            isDownloading = true;

            AsyncHttpInvoker.get(loadUrl, wrapperIpSource.getRecorder(), value -> workThread.post(() -> {
                isDownloading = false;
                Monitor.counter("malenia.IpSource.download", Tags.concat(tags, "success", String.valueOf(value.isSuccess()))).increment();
                if (!value.isSuccess()) {
                    recorder.recordEvent(() -> "ip source download failed", value.e);
                    wrapperIpSource.appendErrorRecord("ip source download failed", value.e);
                    return;
                }
                onDownloadResponse(value.v);
            }));
        });

    }

    @SuppressWarnings("all")
    private static boolean isHTTPLink(String url) {
        return StringUtils.startsWithAny(url, "http://", "https://");
    }

    private void onDownloadResponse(String response) {
        workThread.execute(() -> {
            recorder.recordEvent(() -> "download ip response:\n" + response + "\n");
            List<ProxyIp> proxyIps = wrapperIpSource.getResourceHandler()
                    .parse(response)
                    .stream().filter(proxyIp -> {
                        if (!proxyIp.isValid()) {
                            recorder.recordEvent(() -> "invalid parsed proxyIp");
                            wrapperIpSource.appendErrorRecord("invalid parsed proxyIp:");
                            return false;
                        }
                        return true;
                    }).collect(Collectors.toList());

            Monitor.counter(WrapperIpSource.METRIC_PREFIX + "downloadCount", tags).increment(proxyIps.size());
            Monitor.gauge(WrapperIpSource.METRIC_PREFIX + "downloadSize", tags).set(proxyIps.size());
            if (proxyIps.isEmpty()) {
                wrapperIpSource.appendErrorRecord("download ip source is empty");
                return;
            }
            // fill password from ip source config
            String upUserPassword = wrapperIpSource.getUpUserPassword();
            if (StringUtils.isNotBlank(upUserPassword)) {
                proxyIps.forEach(proxyIpResourceItem -> {
                    if (StringUtils.isBlank(proxyIpResourceItem.getPassword())) {
                        proxyIpResourceItem.setPassword(upUserPassword);
                    }
                });
            }
            String upUserName = wrapperIpSource.getUpUserName();
            if (StringUtils.isNotBlank(upUserName)) {
                proxyIps.forEach(proxyIpResourceItem -> {
                    if (StringUtils.isBlank(proxyIpResourceItem.getUserName())) {
                        proxyIpResourceItem.setUserName(upUserName);
                    }
                });
            }


            Boolean needTest = wrapperIpSource.getNeedTest();
            recorder.recordEvent(() -> "this ip source configure test switch: " + needTest);
            Consumer<DownloadProxyIp> action = BooleanUtils.isFalse(needTest) ?
                    this::offerIpResource :
                    this::runIpQualityTest;

            // 2024年03约21日，医药魔方：
            //  公司降本增效，将malenia服务器压缩到4G内存，同时跑malenia+mysql+python服务，导致物理内存不够
            //  最终排查在这里可能并发发出几百个网络情况（1毫秒内），这导致系统极短时间内存快速分配，进而引发oom
            //  fix此问题使用如下策略：ip下载完成，进行分批延时探测入库，对此网络行为进行削峰填谷，10个ip一批并发多次进行ip质量探测
            //////////////////////////////////////////////////////////////////////////////////////////////////////////
            // step =                 ( interval * 1000    * 0.3)  /  size
            // 步长 =    (加载间隔  * 1000毫秒  * 在前30%时间内完成探测)  /  本次加载数量
            long offerStepInMills = wrapperIpSource.getReloadInterval() * 300 / proxyIps.size();

            new IpOfferStep(workThread, offerStepInMills, proxyIps, action).execute();
        });
    }


    private void offerIpResource(DownloadProxyIp downloadProxyIp) {
        // 请注意，这里必须确保线程正确，因为InetAddress的解析可能比较耗时
        workThread.execute(() -> {
            if (StringUtils.isNotBlank(downloadProxyIp.getOutIp())) {
                try {
                    ipGeoDb.tryCity(InetAddress.getByName(downloadProxyIp.getOutIp())).ifPresent(downloadProxyIp::setCityResponse);
                } catch (Exception e) {
                    recorder.recordEvent("resolve ip geo failed", e);
                }
            } else {
                recorder.recordEvent(() -> "waning: no out_ip resolved, location based ip mapping can not enable");
            }
            recorder.recordEvent(() -> "prepare enpool proxy ip: " + downloadProxyIp);
            wrapperIpSource.getIpPool().offerProxy(downloadProxyIp);
        });

    }

    private void runIpQualityTest(DownloadProxyIp downloadProxyIp) {
        recorder.recordEvent(() -> "[QualityTest] begin test proxy quality: " + downloadProxyIp);
        Recorder recorderForTester = SubscribeRecorders.IP_TEST.acquireRecorder(
                downloadProxyIp.getResourceId() + "_" + System.currentTimeMillis(),
                Environment.isLocalDebug, wrapperIpSource.getSourceKey()
        );
        recorderForTester.recordEvent(() -> "[QualityTest] begin to test proxy:" + downloadProxyIp);


        StringBuilder url = new StringBuilder(Settings.outIpTestUrl.value);
        if (!Settings.outIpTestUrl.value.contains("?")) {
            url.append("?a=1");
        }
        url.append("&nodeId=").append(URLEncoder.encode(ServerIdentifier.id(), StandardCharsets.UTF_8));

        recorder.recordEvent(() -> "[QualityTest] ip test with url: " + url);
        long startTestTimestamp = System.currentTimeMillis();

        // ProxyServer.Builder proxyBuilder = new ProxyServer.Builder(downloadProxyIp.getProxyHost(), downloadProxyIp.getProxyPort());

        ProxyInfo proxyInfo = new ProxyInfo(downloadProxyIp.getProxyHost(), downloadProxyIp.getProxyPort());

        Map<String, String> params = Maps.newHashMap();
        SessionParam.OUTBOUND_USER.set(params, downloadProxyIp.getUserName());
        SessionParam.SESSION_ID.set(params, String.valueOf(ThreadLocalRandom.current().nextInt(1000)));
        IpAuthBuilder.AuthUser authUser = wrapperIpSource.buildAuthUser(params, downloadProxyIp.getPassword());
        if (authUser.hasAuth()) {
            recorder.recordEvent(() -> "[QualityTest] fill compiled authInfo: " + authUser);
            proxyInfo.setUserName(authUser.getUserName());
            proxyInfo.setPassword(authUser.getPassword());
//            proxyBuilder.setRealm(new Realm.Builder(authUser.getUserName(), authUser.getPassword())
//                    .setScheme(Realm.AuthScheme.BASIC));
        }

        if (wrapperIpSource.getSupportProtocolList().stream().anyMatch(Predicate.isEqual(Protocol.SOCKS5))) {
            // 有代理资源只能支持socks5，所以如果代理支持socks5时，直接使用s5来代理，
            // 默认将会使用http协议族，然后malenia具备自动的协议转换能力
            recorder.recordEvent(() -> "[QualityTest] use test protocol SOCKS_V5");
            proxyInfo.setProxyType(Proxy.Type.SOCKS);
        } else {
            recorder.recordEvent(() -> "[QualityTest] use test protocol HTTP");
        }

        AsyncHttpInvoker.get(url.toString(), recorderForTester, proxyInfo, value -> {
            if (value.isSuccess()) {
                if (!IpUtil.isValidIp(value.v)) {
                    // 扭转成功状态，因为响应的内容不是ip，那么认为报文错误
                    Monitor.counter(WrapperIpSource.METRIC_PREFIX + "test.errorResponseContent", tags).increment();
                    value = ValueCallback.Value.failed("response not ip format: " + value.v);
                }
            }
            // 记录监控
            Monitor.counter(WrapperIpSource.METRIC_PREFIX + "test", Tags.concat(tags, "success", String.valueOf(value.isSuccess()))).increment();
            if (!value.isSuccess()) {
                wrapperIpSource.appendErrorRecord("ip test failed", value.e);
                recorder.recordEvent(() -> "[QualityTest] ip test failed", value.e);
                recorderForTester.recordEvent(() -> "ip test failed", value.e);
                wrapperIpSource.getResourceHandler().onProxyIpDrop(downloadProxyIp.getProxyIp(), new CountStatus(), DropReason.IP_TEST_FAILED);
                return;
            }
            recorder.recordEvent(() -> "[QualityTest] ip test success");
            downloadProxyIp.setOutIp(value.v);
            downloadProxyIp.setTestCost(System.currentTimeMillis() - startTestTimestamp);
            offerIpResource(downloadProxyIp);
        });
    }
}
