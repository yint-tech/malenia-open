package cn.iinti.malenia2.service.proxy.core.ssl;


import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.ssl.keygen.KeyStoreGenerator;
import cn.iinti.malenia2.service.proxy.core.ssl.keygen.Networks;
import cn.iinti.malenia2.service.proxy.core.ssl.keygen.PrivateKeyAndCertChain;
import cn.iinti.malenia2.service.proxy.core.ssl.keygen.RootKeyStoreGenerator;
import com.google.common.base.Splitter;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Hold current root cert and cert generator
 *
 * @author Liu Dong
 */
@Slf4j
public class ServerSSLContextManager {

    private KeyStoreGenerator keyStoreGenerator;
    private BigInteger lastRootCertSN;
    // ssl context cache
    private final ConcurrentHashMap<String, SslContext> sslContextCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SslContext> defaultSslContextCache = new ConcurrentHashMap<>();
    // guard for set new root cert
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public static ServerSSLContextManager instance = new ServerSSLContextManager();


    private ServerSSLContextManager() {
        long start = System.currentTimeMillis();
        KeyStoreGenerator keyStoreGenerator;
        try {
            InputStream stream = ServerSSLContextManager.class.getClassLoader()
                    .getResourceAsStream(RootKeyStoreGenerator.DEFAULT_ROOT_KEY_STORE_NAME);
            if (stream == null) {
                throw new IllegalStateException("can not load root keystore");
            }
            keyStoreGenerator = new KeyStoreGenerator(IOUtils.toByteArray(stream), RootKeyStoreGenerator.DEFAULT_ROOT_KEY_STORE_PASSWORD);
        } catch (Exception e) {
            throw new SSLContextException(e);
        }
        log.info("Initialize KeyStoreGenerator cost {} ms", System.currentTimeMillis() - start);
        BigInteger rootCertSN = keyStoreGenerator.getRootCertSN();

        lock.writeLock().lock();
        try {
            if (rootCertSN.equals(lastRootCertSN)) {
                // do nothing
                return;
            }
            this.keyStoreGenerator = keyStoreGenerator;
            this.lastRootCertSN = rootCertSN;
            this.sslContextCache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static final Splitter sp = Splitter.on(",").omitEmptyStrings().trimResults();

    public SslContext selfSSlContext(Recorder recorder) {
        return defaultSslContextCache.computeIfAbsent(StringUtils.trimToEmpty(Settings.autoGenSSLCertificateHostList.value),
                config -> {
                    try {
                        List<String> hosts = sp.splitToList(config.isEmpty() ? "malenia.iinti.cn" : config);
                        PrivateKeyAndCertChain privateKeyAndCertChain = keyStoreGenerator.generateCertChain(hosts, 360, recorder);
                        return SslContextBuilder
                                .forServer(privateKeyAndCertChain.privateKey(), privateKeyAndCertChain.certificateChain())
                                .build();
                    } catch (Exception e) {
                        throw new SSLContextException(e);
                    }
                });
    }


    /**
     * Create ssl context for the host
     */
    public SslContext createProxySSlContext(String host, boolean useH2, Recorder recorder) {
        String finalHost = Networks.wildcardHost(host);
        lock.readLock().lock();
        try {
            return sslContextCache.computeIfAbsent(finalHost + ":" + useH2, key -> {
                try {
                    return getNettySslContextInner(finalHost, useH2, recorder);
                } catch (Exception e) {
                    throw new SSLContextException(e);
                }
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    private SslContext getNettySslContextInner(String host, boolean useH2, Recorder recorder) throws Exception {
        long start = System.currentTimeMillis();
        PrivateKeyAndCertChain keyAndCertChain = keyStoreGenerator.generateCertChain(host, 360, recorder);
        recorder.recordEvent(() -> "Create certificate for " + host + " ,cost " + (System.currentTimeMillis() - start) + " ms");
        SslContextBuilder builder = SslContextBuilder
                .forServer(keyAndCertChain.privateKey(), keyAndCertChain.certificateChain());
        if (useH2) {
//                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            builder.applicationProtocolConfig(new ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1));
        }
        return builder.build();
    }

    public KeyStoreGenerator getKeyStoreGenerator() {
        return keyStoreGenerator;
    }
}
