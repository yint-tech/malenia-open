package cn.iinti.malenia2.service.proxy.core.ssl.keygen;


import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Utils methods for deal with network
 *
 * @author Liu Dong
 */
@Slf4j
public class Networks {

    public static final int HOST_TYPE_IPV6 = 0;
    public static final int HOST_TYPE_IPV4 = 1;
    public static final int HOST_TYPE_DOMAIN = 2;

    /**
     * Ipv4, ipv6, or domain
     */
    public static int getHostType(String host) {
        if (host.contains(":") && !host.contains(".")) {
            return HOST_TYPE_IPV6;
        }
        if (host.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
            return HOST_TYPE_IPV4;
        }
        return HOST_TYPE_DOMAIN;
    }

    /**
     * Generate wildcard host for long domains
     */
    public static String wildcardHost(String host) {
        if (getHostType(host) != HOST_TYPE_DOMAIN) {
            return host;
        }
        String[] items = host.split("\\.");
        if (items.length <= 3) {
            return host;
        }
        return "*." + items[items.length - 3] + "." + items[items.length - 2] + "." + items[items.length - 1];
    }


    public static List<NetworkInfo> getNetworkInfoList() {
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            log.warn("cannot get local network interface ip address", e);
            return Collections.emptyList();
        }
        List<NetworkInfo> list = new ArrayList<>();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            try {
                if (!networkInterface.isUp() || networkInterface.isPointToPoint()) {
                    continue;
                }
            } catch (SocketException e) {
                log.warn("", e);
                continue;
            }
            String name = networkInterface.getName();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (inetAddress instanceof Inet4Address) {
                    String ip = inetAddress.getHostAddress();
                    list.add(new NetworkInfo(name, ip));
                } else if (inetAddress instanceof Inet6Address) {
                    String ip = inetAddress.getHostAddress();
                    list.add(new NetworkInfo(name, ip));
                }
            }
        }
        return list;
    }


    /**
     * For uniq multi cdns, only with different index.
     * img1.fbcdn.com -> img*.fbcdn.com
     */
    public static String genericMultiCDNS(String host) {
        int idx = host.indexOf(".");
        if (idx < 2) {
            return host;
        }
        String first = host.substring(0, idx);
        if (!Character.isLetter(first.charAt(0))) {
            return host;
        }
        char c = first.charAt(first.length() - 1);
        if (!Character.isDigit(c)) {
            return host;
        }
        int firstEnd = first.length() - 2;
        while (Character.isDigit(first.charAt(firstEnd))) {
            firstEnd--;
        }
        return first.substring(0, firstEnd + 1) + "*." + host.substring(idx + 1);
    }

}
