package cn.iinti.malenia2.service.backend;

import cn.iinti.malenia2.entity.UserOrder;
import cn.iinti.malenia2.entity.UserOrderCurrentBandwidth;
import cn.iinti.malenia2.mapper.UserOrderCurrentBandwidthMapper;
import cn.iinti.malenia2.mapper.UserOrderMapper;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.base.safethread.Looper;
import cn.iinti.malenia2.service.proxy.dbconfigs.DbConfigs;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperOrder;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperProduct;
import cn.iinti.malenia2.utils.ServerIdentifier;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CurrentBandWidthService {

    @Resource
    private UserOrderMapper userOrderMapper;

    @Resource
    private UserOrderCurrentBandwidthMapper userOrderCurrentBandwidthMapper;

    private static final Looper workThread = new Looper("current-bandwidth-deduce").startLoop();

    /**
     * 5分钟一次对所有订单的带宽情况进行对账
     * <br/>
     * 需要对账的原因是malenia是允许多台服务器节点一同运行（并且多节点协同是大概率情况），
     * 此时需要把多台服务器的带宽叠加成为订单级别的带宽值
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    private void scheduleDeduce() {
        workThread.execute(() -> accessOrder((userOrder, userOrderCurrentBandwidth) -> {
            WrapperOrder order = DbConfigs.getOrder(userOrder);
            if (order == null) {
                return;
            }

            Double bandwidth = order.taskBandwidth();

            if (userOrderCurrentBandwidth == null) {
                userOrderCurrentBandwidth = new UserOrderCurrentBandwidth();
                userOrderCurrentBandwidth.setCurrentBandwidth(bandwidth);
                userOrderCurrentBandwidth.setServerId(ServerIdentifier.id());
                userOrderCurrentBandwidth.setUserOrderId(order.getId());
                userOrderCurrentBandwidth.setLastUpdateTime(LocalDateTime.now());
                userOrderCurrentBandwidthMapper.insert(userOrderCurrentBandwidth);
            } else {
                userOrderCurrentBandwidth.setLastUpdateTime(LocalDateTime.now());
                userOrderCurrentBandwidth.setCurrentBandwidth(bandwidth);
                userOrderCurrentBandwidthMapper.updateById(userOrderCurrentBandwidth);
            }
        }));
    }


    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 10_000)
    public void scheduleRefreshOrderBandwidth() {
        workThread.execute(() -> {
            Map<String, AtomicDouble> productBandwidth = Maps.newHashMap();
            Map<String, AtomicDouble> userBandwidth = Maps.newHashMap();

            accessOrder((userOrder, userOrderCurrentBandwidth) -> {
                WrapperOrder order = DbConfigs.getOrder(userOrder);
                if (order == null) {
                    return;
                }
                List<UserOrderCurrentBandwidth> userOrderCurrentBandwidths = userOrderCurrentBandwidthMapper.selectList(new QueryWrapper<UserOrderCurrentBandwidth>()
                        .eq(UserOrderCurrentBandwidth.USER_ORDER_ID, order.getId()));

                double bandwidth4Order4Node = userOrderCurrentBandwidths
                        .stream()
                        .filter(it -> it.getServerId().equals(ServerIdentifier.id()))
                        .findFirst()
                        .map(UserOrderCurrentBandwidth::getCurrentBandwidth)
                        .orElse(0D);


                Monitor.gauge(WrapperProduct.METRIC_PREFIX + "bandwidth", Tags.of("user", order.getPurchaseUser(),
                        "productId", order.getProductId())).set(bandwidth4Order4Node);
                productBandwidth.computeIfAbsent(order.getProductId(), (k) -> new AtomicDouble(0)).getAndAdd(bandwidth4Order4Node);
                userBandwidth.computeIfAbsent(order.getPurchaseUser(), (k) -> new AtomicDouble(0)).getAndAdd(bandwidth4Order4Node);

                double bandwidth4Order = userOrderCurrentBandwidths
                        .stream().map(UserOrderCurrentBandwidth::getCurrentBandwidth)
                        .reduce(0d, Double::sum);
                order.reconfigureBandwidthLimit(bandwidth4Order);
            });


            // user分维比较多，如果存在user级别的分维聚合那么计算量比较大，所以提前聚合好再入库，避免渲染的时候针对于很多user做实时的运算，导致的性能问题
            productBandwidth.forEach((productId, atomicDouble) -> Monitor.gauge(WrapperProduct.METRIC_PREFIX + "bandwidthNoUser", Tags.of(
                    "productId", productId)).set(atomicDouble.doubleValue()));

            userBandwidth.forEach((userName, atomicDouble) -> Monitor.gauge(WrapperProduct.METRIC_PREFIX + "bandwidthNoProduct", Tags.of(
                    "user", userName)).set(atomicDouble.get()));
        });
    }


    private interface OrderHandler {
        void accept(UserOrder userOrder, UserOrderCurrentBandwidth userOrderCurrentBandwidth);
    }

    private void accessOrder(OrderHandler orderHandler) {
        List<UserOrder> userOrders = userOrderMapper.selectList(new QueryWrapper<UserOrder>().eq(UserOrder.ENABLED, true));
        if (userOrders.isEmpty()) {
            return;
        }

        Map<Long, UserOrderCurrentBandwidth> configMaps = userOrderCurrentBandwidthMapper.selectList(new QueryWrapper<UserOrderCurrentBandwidth>()
                .eq(UserOrderCurrentBandwidth.SERVER_ID, ServerIdentifier.id())
                .in(UserOrderCurrentBandwidth.USER_ORDER_ID, userOrders
                        .stream()
                        .map(UserOrder::getId)
                        .collect(Collectors.toList()))
        ).stream().collect(Collectors.toMap(UserOrderCurrentBandwidth::getUserOrderId, it -> it));

        for (UserOrder userOrder : userOrders) {
            orderHandler.accept(userOrder, configMaps.get(userOrder.getId()));
        }
    }
}