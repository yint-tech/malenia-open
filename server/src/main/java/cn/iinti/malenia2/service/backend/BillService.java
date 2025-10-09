package cn.iinti.malenia2.service.backend;


import cn.iinti.malenia2.entity.Bill;
import cn.iinti.malenia2.entity.Product;
import cn.iinti.malenia2.entity.UserInfo;
import cn.iinti.malenia2.entity.UserOrder;
import cn.iinti.malenia2.mapper.BillMapper;
import cn.iinti.malenia2.mapper.ProductMapper;
import cn.iinti.malenia2.mapper.UserInfoMapper;
import cn.iinti.malenia2.mapper.UserOrderMapper;
import cn.iinti.malenia2.service.base.BroadcastService;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.base.safethread.Looper;
import cn.iinti.malenia2.service.base.safethread.NumberView;
import cn.iinti.malenia2.service.proxy.dbconfigs.DbConfigs;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperOrder;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperProduct;
import cn.iinti.malenia2.utils.ServerIdentifier;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.collect.Maps;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 核心对账服务，实现用户的扣费和结算
 */
@Slf4j
@Service
public class BillService {
    // 两个对账线程
    private static final Looper hourDeduceThread = new Looper("hourDeduce").startLoop();
    private static final Looper flowDeduceThread = new Looper("rateDeduce").startLoop();
    public static final DateTimeFormatter hourPattern = DateTimeFormatter.ofPattern("yyyyMMdd-HH");

    @Resource
    private UserOrderMapper userOrderMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private BillMapper billMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    @PostConstruct
    private void startRateDeduceTask() {
        flowDeduceThread.postDelay(
                () ->
                        flowDeduceThread.scheduleWithRate(
                                () -> doFareTask(UserOrder.EnumBalanceMethod.METHOD_FLOW),
                                new NumberView(() -> Settings.flowBillDuration.value * 1000 * 60)
                        ),
                30 * 1000
        );
    }


    @PostConstruct
    public void startHourDeduce() {
        // 发现有人钻漏洞，在扣费时间切换计费方式，导致这个账户的整点扣费被略过
        // 所以按小时扣费订单的扣费时间，切换为一个小时多次扣费，且扣费时间不设置在整点，扣费时间间隔增加一定随机
        // 避免用户猜的到按小时任务的指定时间规律
        hourDeduceThread.postDelay(
                () -> hourDeduceThread.scheduleWithRate(
                        () -> doFareTask(UserOrder.EnumBalanceMethod.METHOD_HOUR),
                        27 * 60 * 1000 + ThreadLocalRandom.current().nextInt(10_000)
                ), 40 * 1000);
    }


    @Scheduled(cron = "3 3 3 * * ?")
    public void scheduleDisableOrder() {
        if (!Settings.autoDisableNoneActiveOrder.value) {
            return;
        }
        List<UserOrder> userOrders = userOrderMapper.selectList(new QueryWrapper<UserOrder>().eq(UserOrder.ENABLED, true));
        if (userOrders.isEmpty()) {
            return;
        }
        Map<String, UserInfo> userMap = userInfoMapper.selectList(
                        new QueryWrapper<UserInfo>()
                                .in(UserInfo.USER_NAME, userOrders.stream()
                                        .map(UserOrder::getPurchaseUser)
                                        .collect(Collectors.toSet()))
                ).stream()
                .collect(Collectors.toMap(UserInfo::getUserName, (v) -> v));


        for (UserOrder userOrder : userOrders) {
            UserInfo userInfo = userMap.get(userOrder.getPurchaseUser());
            if (userInfo == null || userInfo.getBalance() > 0) {
                continue;
            }

            long billRecord = billMapper.selectCount(new QueryWrapper<Bill>()
                    .eq(Bill.PURCHASE_USER, userInfo.getUserName())
                    .eq(Bill.PRODUCT_ID, userOrder.getProductId())
                    .ge(Bill.CREATE_TIME, LocalDateTime.now().minusDays(30))
            );
            if (billRecord > 0) {
                continue;
            }

            // 用户没有余额、30日没有扣费记录，证明这个用户已经很久没有用了
            log.info("disable order: {} because none active a longtime", JSONObject.toJSONString(userOrder));

            userOrderMapper.update(null, new UpdateWrapper<UserOrder>().eq(UserOrder.ID, userOrder.getId())
                    .set(UserOrder.ENABLED, false)
            );

        }

    }

    private void doFareTask(UserOrder.EnumBalanceMethod enumBalanceMethod) {
        if (Environment.isLocalDebug) {
            return;
        }
        try {
            doFareTaskNoCheck(enumBalanceMethod);
        } catch (Exception e) {
            log.error("doFareTask error", e);
        }
        BroadcastService.triggerEvent(BroadcastService.Topic.USER);
    }

    private void doFareTaskNoCheck(UserOrder.EnumBalanceMethod enumBalanceMethod) {
        String fareTime = LocalDateTime.now().format(hourPattern);
        log.info("begin of FareTask :{}", fareTime);


        //订单
        List<UserOrder> userOrders = userOrderMapper.selectList(
                new QueryWrapper<UserOrder>().eq(UserOrder.ENABLED, true).eq(UserOrder.BALANCE_METHOD, enumBalanceMethod)
        );

        List<Product> products = productMapper.selectList(new QueryWrapper<Product>().eq(Product.ENABLED, true));
        Map<String, Product> productMap = Maps.newHashMap();
        for (Product product : products) {
            productMap.put(product.getProductId(), product);
        }

        Collections.shuffle(userOrders);

        for (UserOrder userOrder : userOrders) {
            Product product = productMap.get(userOrder.getProductId());
            if (product == null) {
                log.warn("this product:{} has offline ,the fare user:{}", userOrder.getProductId(), userOrder.getPurchaseUser());
                continue;
            }
            doFareOrder(fareTime, userOrder, product);
        }
    }

    private void doFareOrder(String fareTime, UserOrder userOrder, Product product) {
        try {
            log.info("do fare order for order: {}-{} ", userOrder.getPurchaseUser(), product.getProductName());
            doFareOrderUncheck(fareTime, userOrder, product);
        } catch (Exception e) {
            log.error("order fare task error", e);
        }
    }

    private void doFareOrderUncheck(String fareTime, UserOrder userOrder, Product product) {
        UserInfo userInfo = userInfoMapper.selectOne(new QueryWrapper<UserInfo>().eq(UserInfo.USER_NAME, userOrder.getPurchaseUser()));
        log.info("balance method:{} user :{}", userOrder.getBalanceMethod(), userInfo);
        if (userInfo == null) {
            log.error("no user : {} existed", userOrder.getPurchaseUser());
            return;
        }

        double price;
        switch (userOrder.getBalanceMethod()) {
            case METHOD_HOUR:
                price = fareOrderWithHour(fareTime, userOrder, product, userInfo);
                break;
            case METHOD_FLOW:
                price = fareOrderWithRate(fareTime, userOrder, product, userInfo);
                break;
            default:
                log.error("no fare method implementation for: " + userOrder.getBalanceMethod());
                return;
        }
        if (price <= 0D) {
            log.info("the userOrder has no price consumed");
            return;
        }
        Monitor.counter(WrapperProduct.METRIC_PREFIX + "fare",
                "user", userOrder.getPurchaseUser(),
                "method", userOrder.getBalanceMethod().name(),
                "product", product.getProductId()
        ).increment(price);

        log.info("balance consumed price:{}", price);

        userInfoMapper.update(null, new UpdateWrapper<UserInfo>()
                .eq(UserInfo.ID, userInfo.getId())
                .setSql(true, UserInfo.BALANCE + " = " + UserInfo.BALANCE + " -" + price)
        );
    }


    private double fareOrderWithRate(String fareTime, UserOrder userOrder, Product product, UserInfo userInfo) {
        WrapperOrder wrapperOrder = DbConfigs.getOrder(userOrder);
        if (wrapperOrder == null) {
            log.info("can not find order. skip fare");
            return 0D;
        }
        long rate = wrapperOrder.takeFlow();
        log.info("consumed rate: {}", rate);
        FlowPrice price = calc(rate, userOrder.getBalancePrice());
        log.info("flowPrice price:{} count:{}", price.price, price.count);
        if (price.price == 0) {
            wrapperOrder.pushBack((int) rate);
            // 不够扣钱额度，那么一样把消耗推回去
            return 0;
        }
        // 结算会存在误差，所以我们把没有满足结算单位的量滑动到下一个结算时间区间
        long realConsumeRate = (1L << 30) * price.count / 100;
        long feedbackRate = rate - realConsumeRate;
        if (feedbackRate > 0) {
            wrapperOrder.pushBack((int) feedbackRate);
        }
        log.info("realConsumeRate :{} feedbackRate:{} ", realConsumeRate, feedbackRate);

        DbAddHelper.createOrAdd(billMapper,
                new QueryWrapper<Bill>()
                        .eq(Bill.PRODUCT_ID, product.getProductId())
                        .eq(Bill.PURCHASE_USER, userInfo.getUserName())
                        .eq(Bill.BILL_TIME, fareTime),
                () -> createBill(product, userInfo, userOrder, fareTime, price.price),
                Bill.CONSUME_AMOUNT,
                price.price
        );
        return price.price;
    }

    private double fareOrderWithHour(String fareTime, UserOrder userOrder, Product product, UserInfo userInfo) {
        if (userInfo.getBalance() <= 0D) {
            log.info("do not fare with hour when user's balance is not enough:(name={},balance={},product={})",
                    userInfo.getUserName(), userInfo.getBalance(), userOrder.getProductId());
            return 0;
        }
        WrapperOrder wrapperOrder = DbConfigs.getOrder(userOrder);
        if (wrapperOrder != null) {
            wrapperOrder.takeFlow();
        }
        double price = userOrder.getBalancePrice();

        Bill bill = billMapper.selectOne(new QueryWrapper<Bill>()
                .eq(Bill.PRODUCT_ID, product.getProductId())
                .eq(Bill.PURCHASE_USER, userInfo.getUserName())
                .eq(Bill.BILL_TIME, fareTime)
        );
        if (bill != null) {
            // 按时间付费模式下，只需要一台节点产生记录
            return 0;
        }
        try {
            billMapper.insert(createBill(product, userInfo, userOrder, fareTime, price));
        } catch (DuplicateKeyException e) {
            // 如果发生了唯一约束异常，证明其他节点完成了扣费，此时当前节点不能扣费了
            return 0;
        }
        return price;
    }

    private Bill createBill(Product product, UserInfo userInfo, UserOrder userOrder, String fareTime, double price) {
        Bill bill = new Bill();
        bill.setProductId(product.getProductId());
        bill.setProductName(product.getProductName());
        bill.setPurchaseUser(userInfo.getUserName());
        bill.setBalanceMethod(userOrder.getBalanceMethod());
        bill.setBalancePrice(userOrder.getBalancePrice());
        bill.setBillTime(fareTime);
        bill.setConsumeAmount(price);
        bill.setOpNode(ServerIdentifier.id());
        bill.setUserBalance(userInfo.getBalance());
        return bill;
    }


    public static FlowPrice calc(long rate, double balancePrice) {
        FlowPrice flowPrice = new FlowPrice();

        // 计量单位 0.01G
        int count = (int) (rate * 100 >> 30);

        if (count == 0) {
            return flowPrice;
        }
        flowPrice.count = count;
        flowPrice.price = balancePrice * count;
        return flowPrice;
    }

    public static class FlowPrice {
        public int count;
        public double price;
    }
}
