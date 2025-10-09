package cn.iinti.malenia2.service.proxy.core;

import cn.iinti.malenia2.api.ip.resource.SessionParam;
import cn.iinti.malenia2.api.mitm.interceptor.Interceptor;
import cn.iinti.malenia2.service.backend.AccessRecordCollector;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.base.trace.impl.SubscribeRecorders;
import cn.iinti.malenia2.service.proxy.ProxyServer;
import cn.iinti.malenia2.service.proxy.core.outbound.handshark.Protocol;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperOrder;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperProduct;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperUser;
import cn.iinti.malenia2.service.proxy.utils.ConsistentHashUtil;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class Session {

    @Getter
    private final String sessionId = UUID.randomUUID().toString();

    //support socket channel leak check by heap dump
    private final long bornTime = System.currentTimeMillis();

    @Getter
    private SubscribeRecorders.SubscribeRecorder recorder = SubscribeRecorders.USER_SESSION
            .acquireRecorder(sessionId, Environment.isLocalDebug, "default");

    @Getter
    private final Channel inboundChannel;

    @Getter
    private final ProxyServer proxyServer;


    public static Session touch(Channel inboundChannel, ProxyServer proxyServer) {
        return new Session(inboundChannel, proxyServer);
    }

    private Session(Channel inboundChannel, ProxyServer proxyServer) {
        this.inboundChannel = inboundChannel;
        this.proxyServer = proxyServer;

        recorder.recordEvent("new request from: " + inboundChannel);
        attach(inboundChannel);
        inboundChannel.closeFuture().addListener(future -> recorder.recordEvent("user connection closed"));
    }

    private static final AttributeKey<Session> SESSION_ATTRIBUTE_KEY = AttributeKey.newInstance("SESSION_ATTRIBUTE_KEY");

    private void attach(Channel channel) {
        channel.attr(SESSION_ATTRIBUTE_KEY).set(this);
    }

    public static Session get(Channel channel) {
        return channel.attr(SESSION_ATTRIBUTE_KEY).get();
    }


    @Delegate
    private AccessMeta accessMeta;

    public void onAuthFinish(AccessMeta accessMeta) {
        this.accessMeta = accessMeta;
        String traceId = SessionParam.USER_TRACE_ID.get(accessMeta.getSessionParam());

        String userName = accessMeta.getWrapperUser().getUserName();

        // change log
        if (StringUtils.isNotBlank(traceId)) {
            SubscribeRecorders.SubscribeRecorder historyRecorder = recorder;
            recorder.recordEvent(() -> "the trace replaced by :" + traceId);
            recorder = SubscribeRecorders.USER_DEBUG_TRACE
                    .acquireRecorder(sessionId, Environment.isLocalDebug, userName);
            historyRecorder.takeHistory(historyLog -> recorder.recordBatchEvent(historyLog));
            recorder.recordEvent(() -> "start trace record by :" + traceId);
        } else {
            recorder.changeScope("order:" + accessMeta.getWrapperProduct().getProductId() + ":" + accessMeta.getWrapperUser().getUserName(),
                    "product:" + accessMeta.getWrapperProduct().getProductId(),
                    "user:" + accessMeta.getWrapperUser().getUserName()
            );
            recorder.recordEvent(() -> "authed session start->  user: " + userName
                    + " product:" + accessMeta.getWrapperProduct().getProductName()
            );
        }
    }

    public boolean isAuthed() {
        return accessMeta != null;
    }

    public void replay(Channel upstreamChannel) {
        // rate limit && rate data collect
        getWrapperOrder().configureRateLimiter(this, upstreamChannel);

        // 数据转发通道，如果10分钟还没有流量，那么认为是死链，需要关闭通道
        inboundChannel.pipeline().addFirst(new IdleStateHandler(
                0, 0, 600
        ));
        inboundChannel.pipeline().addLast(new SessionIdleChecker());
        NettyUtil.replay(inboundChannel, upstreamChannel, recorder);
    }

    public class SessionIdleChecker extends ChannelInboundHandlerAdapter {

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent event) {
                if (event.state() == IdleState.ALL_IDLE) {
                    recorder.recordEvent("session idle, clean session");
                    ctx.close();
                    return;
                }
            }
            super.userEventTriggered(ctx, evt);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class AccessMeta {
        /**
         * 打日志和编辑内容可能同时发生，所以修改成ConcurrentMap
         *
         * @see SessionParam
         */
        private ConcurrentMap<String, String> sessionParam;
        private WrapperUser wrapperUser;
        private WrapperOrder wrapperOrder;
        private WrapperProduct wrapperProduct;
    }

    @Delegate
    private InProcessMeta inProcessMeta;


    public void onProxyTargetResolved(IpAndPort ipAndPort, Protocol inboundProtocol) {
        String sessionIdKey = SessionParam.SESSION_ID.get(getSessionParam());
        if (StringUtils.isBlank(sessionIdKey)) {
            // todo 行政区域和GEO信息业务需要假如到sessionIdKey中
            sessionIdKey = getWrapperOrder().getRandomTurning() ?
                    String.valueOf(System.currentTimeMillis()) + getProxyServer().getPort() :
                    getWrapperUser().getUserName() + getProxyServer().getPort();
            // 避免透传下游用户名到上游系统，所以这里使用murHash
            SessionParam.SESSION_ID.set(getSessionParam(), String.valueOf(Math.abs(ConsistentHashUtil.murHash(sessionIdKey))));
        } else {
            sessionIdKey = StringUtils.replaceChars(sessionIdKey, '-', 'x');
            sessionIdKey = StringUtils.replaceChars(sessionIdKey, ':', 'x');
            sessionIdKey = StringUtils.replaceChars(sessionIdKey, '_', 'x');
        }

        Long sessionHash = ConsistentHashUtil.murHash(sessionIdKey);

        Interceptor mitmInterceptor = getWrapperUser().findMitmInterceptor(ipAndPort, getWrapperOrder().getProductId());
        inProcessMeta = new InProcessMeta(ipAndPort, sessionHash, inboundProtocol, mitmInterceptor);

        recorder.recordEvent(() -> "proxy target resolved ->  \nsessionHash: " + sessionHash
                + "\ntarget:" + ipAndPort
                + "\ninbound protocol: " + inboundProtocol
                + "\nenable mitm: " + (mitmInterceptor != null)
                + "\n"
        );
        AccessRecordCollector.recordAccess(this);
    }

    @Getter
    @AllArgsConstructor
    public static class InProcessMeta {
        // 代理的最终目标，从代理请求中提取，可以理解为一定存在（只要是合法的代理请求一定能解析到）
        private IpAndPort connectTarget;

        private Long sessionHash;

        private Protocol inboundProtocol;

        @Nullable
        private Interceptor mitmInterceptor;
    }

    public boolean isDirectSend() {
        Interceptor mitmInterceptor = getMitmInterceptor();
        return mitmInterceptor != null && mitmInterceptor.getConfig().isDirectSend();
    }

    public boolean noneMITM() {
        Interceptor mitmInterceptor = getMitmInterceptor();
        return mitmInterceptor == null || mitmInterceptor.getConfig().isDirectSend();
    }
}
