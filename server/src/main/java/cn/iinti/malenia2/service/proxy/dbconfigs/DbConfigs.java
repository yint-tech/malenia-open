package cn.iinti.malenia2.service.proxy.dbconfigs;

import cn.iinti.malenia2.api.ip.resource.SessionParam;
import cn.iinti.malenia2.entity.*;
import cn.iinti.malenia2.service.base.safethread.ValueCallback;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.auth.AuthRules;
import cn.iinti.malenia2.service.proxy.core.Session;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理数据库的配置，连接数据库元数据和代理运行时
 */
@Slf4j
public class DbConfigs {
    /**
     * 所有的用户数据，包括用户基础信息、订单、用量监控等
     */
    private static Map<String, WrapperUser> users = Maps.newHashMap();

    /**
     * 鉴权数据，包括所有用户的账号密码、所有用户的账号白名单等，
     * 此数据具备特定数据结构，用以在常数时间复杂度下命中访问用户，实现快速查找访问用户数据
     */
    private static AuthRules authRules;

    /**
     * 所有的产品信息，产品代表着代理服务器集合，他是对客户服务的一组池化资源，主要应对malenia的用户。
     * malenia的子账户即是访问这些产品资源，malenia内部通过switch完全对上游IpSource的转换
     */
    private static Map<String, WrapperProduct> products = Maps.newHashMap();

    /**
     * 所有的代理IP池，IP池代表malenia的上有资源池，他来自各种代理ip供应商。如代理云、芝麻代理、快代理等
     * malenia通过对IpSource的抽象，完成对所有代理供应商的透明屏蔽
     */
    private static Map<String, WrapperIpSource> ipSources = Maps.newHashMap();

    /**
     * @param allUserInfo  用户数据
     * @param allOrders    订单数据
     * @param authWhiteIps 白名单数据
     * @param mitmScripts  中间人攻击脚本
     */
    public static void reloadUserConfig(List<UserInfo> allUserInfo, List<UserOrder> allOrders,
                                        List<AuthWhiteIp> authWhiteIps, List<MitmScript> mitmScripts) {
        HashMultimap<String, UserOrder> orderMultipleMap = HashMultimap.create();
        for (UserOrder userOrder : allOrders) {
            orderMultipleMap.put(userOrder.getPurchaseUser(), userOrder);
        }
        HashMultimap<String, AuthWhiteIp> whiteIpMultipleMap = HashMultimap.create();
        for (AuthWhiteIp authWhiteIp : authWhiteIps) {
            whiteIpMultipleMap.put(authWhiteIp.getBindUser(), authWhiteIp);
        }
        HashMultimap<String, MitmScript> mitmScriptHashMultimap = HashMultimap.create();
        for (MitmScript mitmScript : mitmScripts) {
            mitmScriptHashMultimap.put(mitmScript.getUser(), mitmScript);
        }

        Map<String, WrapperUser> copyUsers = Maps.newHashMap(users);
        Map<String, WrapperUser> newUsers = Maps.newHashMap();
        AuthRules newAuthRules = new AuthRules();

        for (UserInfo userInfo : allUserInfo) {
            Set<UserOrder> userOrders = orderMultipleMap.get(userInfo.getUserName());
            // 2024/01/02 未采购任何订单的用户无法鉴权成功，
            // 故所有用户都应该加载auth
            //            if (userOrders.isEmpty()) {
            //                // 当前用户只是注册用户，他没有采购任何产品
            //                continue;
            //            }

            WrapperUser wrapperUser = copyUsers.computeIfAbsent(userInfo.getUserName(), (k) -> new WrapperUser(userInfo))
                    .reloadBasic(userInfo)
                    .reloadOrders(userOrders)
                    .reloadMitmScript(mitmScriptHashMultimap.get(userInfo.getUserName()));

            newUsers.put(userInfo.getUserName(), wrapperUser);

            newAuthRules.addUserNameConfig(wrapperUser.getResolvedAuthAccount(), wrapperUser.getResolvedAuthPassword(), wrapperUser);
            whiteIpMultipleMap.get(userInfo.getUserName())
                    .forEach(authWhiteIp -> newAuthRules.addCidrIpConfig(authWhiteIp.getWhiteIp(), wrapperUser));

        }
        users = newUsers;
        authRules = newAuthRules;
    }

    public static WrapperUser getUser(UserInfo userInfo) {
        return users.get(userInfo.getUserName());
    }

    public static WrapperOrder getOrder(String userName, String productId) {
        if (userName == null || productId == null) {
            return null;
        }
        WrapperUser wrapperUser = users.get(userName);
        if (wrapperUser == null) {
            return null;
        }
        return wrapperUser.orders.get(productId);
    }

    public static WrapperOrder getOrder(UserOrder userOrder) {
        WrapperUser wrapperUser = users.get(userOrder.getPurchaseUser());
        if (wrapperUser == null) {
            return null;
        }
        return wrapperUser.orders.get(userOrder.getProductId());
    }

    public static void doAuth(Session session, String userName, String password) {
        ConcurrentHashMap<String, String> sessionParam = SessionParam.parseUserParam(userName);
        userName = SessionParam.INBOUND_USER.get(sessionParam);

        Recorder recorder = session.getRecorder();
        WrapperUser wrapperUser = queryAccessUser(session, userName, password);
        if (wrapperUser == null) {
            recorder.recordEvent(() -> "this session not auth");
            return;
        }
        recorder.recordEvent(() -> "final authed user:" + wrapperUser.getUserName());

        WrapperProduct wrapperProduct = session.getProxyServer().getWrapperProduct();
        String productId = wrapperProduct.getProductId();
        WrapperOrder wrapperOrder = wrapperUser.orders.get(productId);

        session.onAuthFinish(new Session.AccessMeta(sessionParam, wrapperUser, wrapperOrder, wrapperProduct));
    }

    private static WrapperUser queryAccessUser(Session session, String userName, String password) {
        Recorder recorder = session.getRecorder();
        WrapperUser userInfo = null;
        if (StringUtils.isNoneBlank(userName, password)) {
            userInfo = authRules.doAuth(userName, password);
            recorder.recordEvent(userInfo, (user) -> "auth with userName: " + userName + " password:" + password + " result: " + (user != null));
        }

        if (userInfo == null) {
            SocketAddress socketAddress = session.getInboundChannel().remoteAddress();
            String remoteIp = ((InetSocketAddress) socketAddress).getHostString();
            userInfo = authRules.doAuth(remoteIp);
            recorder.recordEvent(userInfo, (user) -> "auth with cidr ip :" + remoteIp + " result: " + (user != null));
        }
        return userInfo;
    }

    public static WrapperUser testCidrIp(String ipConfig) {
        return authRules.doAuth(ipConfig);
    }


    public static void refreshProduct(List<Product> productList, List<ProductSource> relation) {
        Multimap<String, ProductSource> productIpSourceMap = HashMultimap.create();
        for (ProductSource productSource : relation) {
            productIpSourceMap.put(productSource.getProductId(), productSource);
        }

        Map<String, WrapperProduct> needOfflineProduct = Maps.newHashMap(products);
        Map<String, WrapperProduct> copyOnWriteMap = Maps.newHashMap();
        for (Product product : productList) {
            // 当前ip的所有ip源
            Collection<ProductSource> sources = productIpSourceMap.get(product.getProductId());
            if (CollectionUtils.isEmpty(sources)) {
                log.warn("no ipSource binding on product: {} ->:{}", product.getProductId(), product.getProductName());
                continue;
            }
            Map<String, Integer> ipSourceWithRatio = Maps.newHashMap();
            for (ProductSource productSource : sources) {
                ipSourceWithRatio.put(productSource.getSourceKey(), productSource.getRatio());
            }

            WrapperProduct wrapperProduct = products.get(product.getProductId());
            if (wrapperProduct != null) {
                needOfflineProduct.remove(product.getProductId());
            } else {
                wrapperProduct = new WrapperProduct();
            }
            wrapperProduct.reload(product).reloadIpSourceRatio(ipSourceWithRatio);
            copyOnWriteMap.put(product.getProductId(), wrapperProduct);
        }
        products = copyOnWriteMap;

        for (WrapperProduct mirrorProduct : needOfflineProduct.values()) {
            mirrorProduct.destroy();
        }
    }

    public static Set<Integer> listenPorts(String skipProductId) {
        Set<Integer> portSets = Sets.newHashSet();
        for (WrapperProduct product : products.values()) {
            if (product.getProductId().equals(skipProductId)) {
                continue;
            }
            portSets.addAll(product.usedPorts());
        }
        return portSets;
    }

    public static TreeSet<Integer> productUsedPorts(String productId) {
        TreeSet<Integer> ret = Sets.newTreeSet();
        WrapperProduct wrapperProduct = products.get(productId);
        if (wrapperProduct != null) {
            ret.addAll(wrapperProduct.usedPorts());
        }
        return ret;
    }

    public static void reloadIpSource(List<IpSource> ipSourceList) {
        Map<String, WrapperIpSource> copyOnWrite = Maps.newHashMap();
        Map<String, WrapperIpSource> needOffline = Maps.newHashMap(ipSources);

        for (IpSource ipSource : ipSourceList) {
            String sourceKey = ipSource.getSourceKey();
            WrapperIpSource wrapperIpSource = ipSources.get(sourceKey);
            if (wrapperIpSource == null) {
                wrapperIpSource = new WrapperIpSource(ipSource);
            } else {
                wrapperIpSource.reload(ipSource);
            }
            copyOnWrite.put(sourceKey, wrapperIpSource);
            needOffline.remove(sourceKey);
        }

        // 副本替换，之后下线
        ipSources = copyOnWrite;

        // offline old IpSource
        for (WrapperIpSource mirrorIpSource : needOffline.values()) {
            mirrorIpSource.destroy();
        }
    }

    public static Collection<String> ipSourceList() {
        return ipSources.keySet();
    }

    /**
     * IP池健康指数，正常值为100，最大值一般不超过150，小于100认为不健康，最低为0（代表此IP资源已经完全挂了）
     *
     * @param ipSourceKey IP资源id
     * @return 当前IP资源的健康指数，如IP资源不存在，则返回null
     */
    public static Double ipSourceHealthScore(String ipSourceKey) {
        WrapperIpSource wrapperIpSource = ipSources.get(ipSourceKey);
        if (wrapperIpSource == null) {
            return null;
        }
        return wrapperIpSource.healthScore();
    }

    public static boolean isValidIpSource(WrapperIpSource wrapperIpSource) {
        return wrapperIpSource != null && !wrapperIpSource.poolEmpty();
    }

    public static void allocateIpSource(Recorder recorder, String tag, String prefer, List<String> candidate,
                                        ValueCallback<WrapperIpSource> valueCallback) {
        WrapperIpSource wrapperIpSource = ipSources.get(prefer);

        if (wrapperIpSource == null) {
            valueCallback.onReceiveValue(ValueCallback.Value.failed("no ipSources: " + prefer + " defined"));
            return;
        }
        if (isValidIpSource(wrapperIpSource)) {
            ValueCallback.success(valueCallback, wrapperIpSource);
            return;
        }
        if (ipSources.size() > 1) {
            recorder.recordEvent(() -> tag + "IpSourceManager this ipSource not have ip resource, begin to malenia to next ip source");
            int length = candidate.size();
            int start = Math.abs(prefer.hashCode()) + candidate.size();
            for (int i = 0; i < length; i++) {
                String newSourceKey = candidate.get((start + i) % candidate.size());
                if (newSourceKey.equals(prefer)) {
                    continue;
                }
                wrapperIpSource = ipSources.get(newSourceKey);
                if (isValidIpSource(wrapperIpSource)) {
                    ValueCallback.success(valueCallback, wrapperIpSource);
                    return;
                }
            }
        }

        ValueCallback.failed(valueCallback, "can not find online IpSourceKey");
    }

    public static CommonRes<List<VoIpItem>> getResourceContentList(String ipSourceKey) {
        WrapperIpSource wrapperIpSource = ipSources.get(ipSourceKey);
        if (wrapperIpSource == null) {
            return CommonRes.success(Collections.emptyList());
        }
        return CommonRes.success(wrapperIpSource.getIpPool().resourceContentList());
    }

    public static String getResourceLastErrorRecords(String ipSourceKey) {
        WrapperIpSource wrapperIpSource = ipSources.get(ipSourceKey);
        if (wrapperIpSource == null) {
            return "no ipSources: " + ipSourceKey;
        }
        return wrapperIpSource.getLastErrors();
    }
}
