package cn.iinti.malenia2.api.ip.resource;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class IpResourceHelper {
    public static ProxyIp fromIpPort(String ipAndPort) {
        return setupIpPort(new ProxyIp(), ipAndPort);
    }

    public static ProxyIp fromIpPort(String ip, int port) {
        ProxyIp proxyIp = new ProxyIp();
        proxyIp.setProxyHost(ip);
        proxyIp.setProxyPort(port);
        return proxyIp;
    }

    static ProxyIp setupIpPort(ProxyIp proxyIp, String ipAndPort) {
        ipAndPort = ipAndPort.trim();
        if (!ipAndPort.contains(":")) {
            return proxyIp;
        }
        String[] split = ipAndPort.split(":");
        proxyIp.setProxyHost(split[0].trim());
        proxyIp.setProxyPort(NumberUtils.toInt(split[1].trim(), -1));
        return proxyIp;
    }

    public static String check(ProxyIp proxyIp) {
        String ip = proxyIp.getProxyHost();
        if (StringUtils.isBlank(ip)) {
            return "ip can not empty";
        }
        ip = ip.trim();
        proxyIp.setProxyHost(ip);
        if (!isIpV4(ip)) {
            try {
                InetAddress byName = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                return "error domain:" + e.getMessage();
            }
        }

        Integer port = proxyIp.getProxyPort();
        if (port == null || port <= 0 || port > 65535) {
            return "port range error";
        }

        return null;
    }

    private static final Splitter dotSplitter = Splitter.on('.');

    public static boolean isIpV4(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }
        // 3 * 4 + 3 = 15
        // 1 * 4 + 3 = 7
        if (input.length() > 15 || input.length() < 7) {
            return false;
        }

        List<String> split = dotSplitter.splitToList(input);
        if (split.size() != 4) {
            return false;
        }
        for (String segment : split) {
            int i = NumberUtils.toInt(segment, -1);
            if (i < 0 || i > 255) {
                return false;
            }
        }
        return true;
    }
}
