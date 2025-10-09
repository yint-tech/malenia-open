package cn.iinti.malenia2.service.proxy.auth;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IpTrie<T> {
    private IpTrie<T> left;
    private IpTrie<T> right;
    private Set<T> additions = new HashSet<>();
    private static final String localhostStr = "localhost";
    private static final String localhost = "127.0.0.1";


    public IpTrie<T> copy() {
        IpTrie<T> other = new IpTrie<>();
        other.additions = new HashSet<>(additions);
        if (left != null) {
            left = left.copy();
        }
        if (right != null) {
            right = right.copy();
        }
        return other;
    }

    public void insert(String ipConfig, T addition) {
        String ip;
        int cidr = 32;
        if (ipConfig.contains("/")) {
            String[] split = ipConfig.split("/");
            ip = StringUtils.trim(split[0]);
            cidr = Integer.parseInt(StringUtils.trim(split[1]));
        } else {
            ip = ipConfig.trim();
        }

        insert(0, ip2Int(ip), cidr, addition);
    }

    private void insert(int deep, long ip, int cidr, T addition) {
        if (deep >= cidr) {
            synchronized (this) {
                additions.add(addition);
            }
            return;
        }

        int bit = (int) ((ip >>> (32 - deep)) & 0x01);
        if (bit == 0) {
            if (left == null) {
                left = new IpTrie<>();
            }
            left.insert(deep + 1, ip, cidr, addition);
        } else {
            if (right == null) {
                right = new IpTrie<>();
            }
            right.insert(deep + 1, ip, cidr, addition);
        }
    }

    private static long ip2Int(String ip) {
        if (localhostStr.equals(ip)) {
            ip = localhost;
        }
        String[] split = ip.split("\\.");
        return ((Long.parseLong(split[0]) << 24
                | Long.parseLong(split[1]) << 16
                | Long.parseLong(split[2]) << 8
                | Long.parseLong(split[3])));
    }

    public T find(String ip) {
        Set<T> strings = find(ip, null);
        if (strings == null) {
            return null;
        }
        if (strings.isEmpty()) {
            return null;
        }
        return strings.iterator().next();
    }

    public Set<T> find(String ip, T addition) {
        if (ip.contains("/")) {
            // 这里可能输入了一个cidr的ip规则
            ip = ip.substring(0, ip.indexOf('/'));
        }

        return find(ip2Int(ip), 0, addition);
    }


    private Set<T> find(long ip, int deep, T addition) {
        if (addition != null && additions.contains(addition)) {
            HashSet<T> ts = new HashSet<>();
            ts.add(addition);
            return ts;
        }
        if (!additions.isEmpty()) {
            return Collections.unmodifiableSet(additions);
        }
        int bit = (int) ((ip >>> (32 - deep)) & 0x01);
        if (bit == 0) {
            if (left == null) {
                return null;
            }
            return left.find(ip, deep + 1, addition);
        } else {
            if (right == null) {
                return null;
            }
            return right.find(ip, deep + 1, addition);
        }
    }

    public void remove(String ipConfig, T addition) {
        String ip;
        int cidr = 32;
        if (ipConfig.contains("/")) {
            String[] split = ipConfig.split("/");
            cidr = Integer.parseInt(StringUtils.trim(split[1]));
            ip = StringUtils.trim(split[0]);
        } else {
            ip = ipConfig.trim();
        }
        remove(0, ip2Int(ip), cidr, addition);
    }

    private void remove(int deep, long ip, int cidr, T addition) {
        if (deep >= cidr) {
            synchronized (this) {
                additions.remove(addition);
            }
            return;
        }

        int bit = (int) ((ip >>> (32 - deep)) & 0x01);
        if (bit == 0) {
            if (left == null) {
                return;
            }
            left.remove(deep + 1, ip, cidr, addition);
        } else {
            if (right == null) {
                return;
            }
            right.remove(deep + 1, ip, cidr, addition);
        }
    }

    public int remove4Account(T addition) {
        int nodeSize = 0;
        if (left != null) {
            int i = left.remove4Account(addition);
            if (i == 0) {
                left = null;
            }
            nodeSize += i;
        }
        if (right != null) {
            int i = right.remove4Account(addition);
            nodeSize += i;
            if (i == 0) {
                right = null;
            }
        }
        synchronized (this) {
            additions.remove(addition);
        }
        return nodeSize + additions.size();
    }
}
