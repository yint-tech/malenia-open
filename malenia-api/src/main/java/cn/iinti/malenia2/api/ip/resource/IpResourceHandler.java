package cn.iinti.malenia2.api.ip.resource;


import java.util.Collections;
import java.util.List;

/**
 * 给外部资源提供的编程适配入口
 */
public interface IpResourceHandler extends IpAuthBuilder {
    /**
     * 将一个文本解析为IP资源，常用与适配非标准的ip资源配置列表
     */
    default List<ProxyIp> parse(String responseText) {
        return Collections.emptyList();
    }

    /**
     * 代理ip资源下线（丢弃）时回调，此时用户可以扩展对ip资源的管理操作，如：
     * <ul>
     *     <li>IP资源供应为提供拨号能力的VPS节点，则此时发起vps节点重播，用于上线新的ip资源</li>
     *     <li>统计本资源的使用情况：如成功/失败信息、机房分布信息、城市分布信息、流量情况等</li>
     * </ul>
     * 如下时机系统触发ip资源丢弃调用
     * <ul>
     *     <li>本ip资源可用性探测失败</li>
     *     <li>本IP资源加入ip池，但是使用期间被动态判定ip质量差，而触发的动态下线</li>
     *     <li>本ip资源加入ip池，但是使用期间明确得知ip已经无法使用：如Connect Refuse异常</li>
     *     <li>本ip资源可用，但是达到了用户自己设定的最长存活时间</li>
     *     <li>本ip可用，但是ip资源池资源非常充足，从而被新的IP资源替换挤出</li>
     * </ul>
     * 非常重要！！ 此回调内不允许执行耗时任务，如阻塞线程 ！！ ，本扩展给管理员使用，故没有考虑此行为带来的性能影响问题
     * 如您需要发送外部http请求给其他系统，则应该使用异步HttpApi {@link AsyncHttp}
     *
     * @param proxyIp     ip实体，来自于上一步的parse
     * @param countStatus 服务供应统计
     * @param dropReason  下线原因，给业务做逻辑判定使用
     */
    default void onProxyIpDrop(ProxyIp proxyIp, CountStatus countStatus, DropReason dropReason) {
    }
}
