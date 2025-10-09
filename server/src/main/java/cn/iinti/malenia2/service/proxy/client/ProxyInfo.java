package cn.iinti.malenia2.service.proxy.client;

import lombok.Getter;
import lombok.Setter;

import java.net.Proxy;

@Getter
public class ProxyInfo {
    private final String host;
    private final int port;
    @Setter
    private Proxy.Type proxyType = Proxy.Type.HTTP;

    @Setter
    private String userName;

    @Setter
    private String password;

    public ProxyInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

}
