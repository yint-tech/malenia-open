package cn.iinti.malenia2.service.proxy.dbconfigs;

import cn.iinti.malenia2.api.mitm.interceptor.Interceptor;
import cn.iinti.malenia2.entity.MitmScript;
import cn.iinti.malenia2.entity.UserInfo;
import cn.iinti.malenia2.entity.UserOrder;
import cn.iinti.malenia2.service.proxy.core.IpAndPort;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class WrapperUser {

    @Delegate
    @Getter
    private UserInfo userInfo;

    @Getter
    private String resolvedAuthAccount;

    @Getter
    private String resolvedAuthPassword;


    public Map<String, WrapperOrder> orders = Maps.newHashMap();

    private List<WrapperMitmScript> mitmScriptList = Lists.newArrayList();


    public WrapperUser(UserInfo userInfo) {
        this.userInfo = userInfo;
    }


    public WrapperUser reloadBasic(UserInfo userInfo) {
        this.userInfo = userInfo;
        if (StringUtils.isNoneBlank(userInfo.getAuthAccount(), userInfo.getAuthPwd())) {
            resolvedAuthAccount = userInfo.getAuthAccount();
            resolvedAuthPassword = userInfo.getAuthPwd();
        } else {
            resolvedAuthAccount = userInfo.getUserName();
            resolvedAuthPassword = userInfo.getPassword();
        }
        return this;
    }


    public WrapperUser reloadOrders(Set<UserOrder> userOrders) {
        Map<String, WrapperOrder> newOrders = Maps.newConcurrentMap();
        HashMap<String, WrapperOrder> copyOrders = new HashMap<>(orders);
        userOrders.forEach(userOrder -> {
            String productId = userOrder.getProductId();
            WrapperOrder wrapperOrder = copyOrders.computeIfAbsent(productId, k -> new WrapperOrder(userOrder))
                    .reloadDb(userOrder);
            newOrders.put(productId, wrapperOrder);
        });
        orders = newOrders;
        return this;
    }

    public WrapperUser reloadMitmScript(Set<MitmScript> mitmScripts) {
        if (mitmScripts == null) {
            return this;
        }

        Map<Long, WrapperMitmScript> old = mitmScriptList.stream()
                .collect(Collectors.toMap((Function<WrapperMitmScript, Long>) WrapperMitmScript::getId,
                        (Function<WrapperMitmScript, WrapperMitmScript>) input -> input));

        mitmScriptList = mitmScripts.stream()
                .map((Function<MitmScript, WrapperMitmScript>) input ->
                        old.computeIfAbsent(input.getId(), (id) -> new WrapperMitmScript())
                                .reload(input))
                .sorted(Comparator.comparing(WrapperMitmScript::getPriority))
                .collect(Collectors.toList());
        return this;
    }

    public Interceptor findMitmInterceptor(IpAndPort ipAndPort, String productId) {
        for (WrapperMitmScript wrapperMitmScript : mitmScriptList) {
            Interceptor interceptor = wrapperMitmScript.findInterceptor(ipAndPort.getIp(), ipAndPort.getPort(), productId);
            if (interceptor != null) {
                return interceptor;
            }
        }
        return null;
    }

}
