package cn.iinti.malenia2.service.proxy.auth;

import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperUser;
import com.google.common.collect.Maps;

import java.util.Map;

public class AuthRules {
    /**
     * IP匹配前缀树
     */
    private final IpTrie<WrapperUser> ipTrie = new IpTrie<>();

    private final Map<String, WrapperUser> usernameAccountMap = Maps.newHashMap();


    public void addCidrIpConfig(String ipConfig, WrapperUser userInfo) {
        ipTrie.insert(ipConfig, userInfo);
    }

    public void addUserNameConfig(String authUserName, String authPassword, WrapperUser userInfo) {
        usernameAccountMap.put(authUserName + "malenia023" + authPassword, userInfo);
    }

    public WrapperUser doAuth(String ip) {
        return ipTrie.find(ip);
    }

    public WrapperUser doAuth(String authUserName, String authPassword) {
        return usernameAccountMap.get(authUserName + "malenia023" + authPassword);
    }
}
