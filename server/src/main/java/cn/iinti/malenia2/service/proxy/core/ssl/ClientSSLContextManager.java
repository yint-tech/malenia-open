package cn.iinti.malenia2.service.proxy.core.ssl;


import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Getter;

import javax.net.ssl.SSLException;

public class ClientSSLContextManager {

    @Getter
    private final SslContext noneHttp2Context = createNettyClientSSlContextNoHttp2();

    @Getter
    private final SslContext context = createNettyClientSSlContext();

    private static final ClientSSLContextManager instance = new ClientSSLContextManager();

    public static ClientSSLContextManager getInstance() {
        return instance;
    }

    private static SslContext createNettyClientSSlContext() {
        try {
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1));
            if (OpenSsl.isAvailable()) {
                sslContextBuilder.sslProvider(SslProvider.OPENSSL);
            } else {
                sslContextBuilder.sslProvider(SslProvider.JDK);
            }
            return sslContextBuilder.build();
        } catch (SSLException e) {
            throw new SSLContextException(e);
        }
    }

    private static SslContext createNettyClientSSlContextNoHttp2() {
        try {
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE);
            if (OpenSsl.isAvailable()) {
                sslContextBuilder.sslProvider(SslProvider.OPENSSL);
            } else {
                sslContextBuilder.sslProvider(SslProvider.JDK);
            }
            return sslContextBuilder.build();
        } catch (SSLException e) {
            throw new SSLContextException(e);
        }
    }

}
