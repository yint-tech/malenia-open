package cn.iinti.malenia2.service.proxy.dbconfigs;

import cn.iinti.malenia2.entity.UserOrder;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import cn.iinti.malenia2.service.proxy.NettyThreadPools;
import cn.iinti.malenia2.service.proxy.core.Session;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tags;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.MaleniaGlobalTrafficShapingHandler;
import lombok.experimental.Delegate;

import java.util.concurrent.atomic.AtomicLong;

public class WrapperOrder {

    @Delegate
    private UserOrder userOrder;

    /**
     * 该订单当前待扣费流量(按流量付费订单，每当完成对账，则会扣减流量。完成用户余额和用户消费的数据一致性同步)
     * 但是请注意流量付费一般来说都是有延迟的。我们一般做到5分钟级别对账即可，极限用户可以白嫖5分钟的流量无上限时间
     */
    private final AtomicLong unBillFlow = new AtomicLong();


    /**
     * bandwidth
     */
    private final AtomicLong bandwidthBillFow = new AtomicLong();

    private long bandwidthCountStart = System.currentTimeMillis() / 1000;

    private final BandwidthLimiter bandwidthLimiter;

    public WrapperOrder(UserOrder initialOrder) {
        this.userOrder = initialOrder;
        this.bandwidthLimiter = new BandwidthLimiter(initialOrder);
    }

    public WrapperOrder reloadDb(UserOrder userOrder) {
        this.userOrder = userOrder;
        return this;
    }

    /**
     * 记录使用量
     *
     * @param flowByteSize 字节数量
     */
    public void recordUsage(int flowByteSize) {
        unBillFlow.addAndGet(flowByteSize);
        bandwidthBillFow.addAndGet(flowByteSize);
    }

    public void pushBack(int unBill) {
        unBillFlow.addAndGet(unBill);
    }

    /**
     * 拉取流量消费记录，请注意不要重复拉取。
     *
     * @return 流量
     */
    public long takeFlow() {
        long value = unBillFlow.get();
        unBillFlow.addAndGet(-value);
        return value;
    }

    /**
     * 拉取当前账单的宽带统计数据，请注意拉取后，计数重新开始
     *
     * @return 平均每秒占用的带宽（字节/秒）
     */
    public Double taskBandwidth() {
        long now = System.currentTimeMillis() / 1000;
        if (now == bandwidthCountStart) {
            return 0D;
        }

        long value = bandwidthBillFow.get();
        bandwidthBillFow.addAndGet(-value);


        double nowBandwidth = value * 1.0 / (now - bandwidthCountStart);

        bandwidthLimiter.serverNodeBandwidth = nowBandwidth;

        bandwidthCountStart = now;
        return nowBandwidth;
    }

    public void reconfigureBandwidthLimit(Double orderBandwidth) {
        bandwidthLimiter.reconfigureBandwidthLimit(orderBandwidth);
    }

    public void configureRateLimiter(Session session, Channel upstreamChannel) {
        GlobalTrafficShapingHandler globalTrafficShapingHandler = upstreamChannel.pipeline().get(GlobalTrafficShapingHandler.class);
        if (globalTrafficShapingHandler != null) {
            return;
        }
        ChannelPipeline pipeline = upstreamChannel.pipeline();
        pipeline.addFirst(new RateMonitor(session), bandwidthLimiter.globalTrafficShapingHandler);
    }

    class RateMonitor extends ChannelInboundHandlerAdapter {
        private final Session session;
        private final Counter counter;

        public RateMonitor(Session session) {
            this.session = session;
            Tags tags = Tags.of("user", session.getWrapperUser().getUserName(),
                    "productId", session.getWrapperProduct().getProductId(),
                    "inboundProtocol", session.getInboundProtocol().name()
            );
            counter = Monitor.counter(WrapperProduct.METRIC_PREFIX + "flow", tags);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            WrapperUser wrapperUser = session.getWrapperUser();
            if (wrapperUser.getBalance() <= 0) {
                // 如果用户在通过系统下载大文件，整个网络请求是一个长任务，此时可能在下载了很久之后，用户已经没钱了
                // 所以这里需要在运行时再check一下用户是否有余额
                session.getRecorder().recordEvent(() -> "interrupt access because of no balance left for user:" + wrapperUser.getBalance());
                ctx.close();
                return;
            }

            try {
                if (msg instanceof ByteBuf) {
                    int bytes = ((ByteBuf) msg).readableBytes();
                    recordUsage(bytes);
                    counter.increment(bytes);
                }
            } catch (Exception e) {
                session.getRecorder().recordEvent("handle rate data monitor failed", e);
            } finally {
                super.channelRead(ctx, msg);
            }


        }
    }


    class BandwidthLimiter {
        private static final long M = 1024 * 1024;

        private static final long UN_LIMIT = 10240 * M;


        public Double orderBandwidth = 0d;
        public Double serverNodeBandwidth = 0d;

        private final MaleniaGlobalTrafficShapingHandler globalTrafficShapingHandler;

        public BandwidthLimiter(UserOrder initialOrder) {
            double limit = initialOrder.getBalanceMethod() == UserOrder.EnumBalanceMethod.METHOD_FLOW ? UN_LIMIT :
                    initialOrder.bandwidthLimitWithByte();

            // 只限制读，但是不限制写，看了下内部实现，
            // 读通过socket autoRead功能实现，数据在tcp/ip协议栈中，所以可以被tcp限流机制打压
            // 写则通过数据驻留在内存，延时发送的方式，将会占用内存资源
            this.globalTrafficShapingHandler = new MaleniaGlobalTrafficShapingHandler(
                    NettyThreadPools.bandwidthLimitWorkGroup.next(), UN_LIMIT, (long) limit
            );
        }

        public void reconfigureBandwidthLimit(Double orderCurrentBandwidth) {
            this.orderBandwidth = orderCurrentBandwidth;

            if (getBalanceMethod() == UserOrder.EnumBalanceMethod.METHOD_FLOW) {
                // 按流量付费模式下，不进行限流
                globalTrafficShapingHandler.setReadLimit(UN_LIMIT);
                return;
            }

            // 给用户送10%的带宽
            long orderLimit = (long) (bandwidthLimitWithByte() * 1.1);

            if (orderCurrentBandwidth > orderLimit) {
                // 订单占用带宽超过了限制，此时需要打压
                double ratio = orderLimit * 1.2 / orderCurrentBandwidth;
                if (ratio > 0.9) {
                    ratio = 0.9;
                }
                long newLimit = (long) (globalTrafficShapingHandler.getReadLimit() * ratio);
                if (newLimit > 100) {
                    globalTrafficShapingHandler.setReadLimit(newLimit);
                }
            } else if (orderCurrentBandwidth < orderLimit) {
                // 订单带宽没有达到限制，此时放开打压，但是最高不能超过订单的最高配额
                // orderCurrentBandwidth / orderLimit = nowNodeConfig / nodeLimit
                // then the formula is :
                // nodeLimit = orderLimit * nowNodeConfig / orderCurrentBandwidth
                long nowNodeConfig = globalTrafficShapingHandler.getReadLimit();
                long newLimit = orderCurrentBandwidth <= 0 ? orderLimit : (long) (orderLimit * nowNodeConfig / orderCurrentBandwidth);
                if (newLimit > orderLimit) {
                    newLimit = orderLimit;
                }
                if (newLimit != nowNodeConfig) {
                    globalTrafficShapingHandler.setReadLimit(newLimit);
                }
            }
        }


    }
}
