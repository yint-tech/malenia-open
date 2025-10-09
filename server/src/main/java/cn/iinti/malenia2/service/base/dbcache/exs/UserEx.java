package cn.iinti.malenia2.service.base.dbcache.exs;

import cn.iinti.malenia2.entity.UserInfo;
import cn.iinti.malenia2.service.base.perm.PermsService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class UserEx {
    public Map<String, Collection<String>> perms = Collections.emptyMap();

    private String prePermsConfig = null;

    public void reload(UserInfo userInfo) {
        if (Objects.equals(prePermsConfig, userInfo.getPermission())) {
            return;
        }
        prePermsConfig = userInfo.getPermission();
        perms = PermsService.parseExp(prePermsConfig, true);
    }
}
