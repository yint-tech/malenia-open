package cn.iinti.malenia2.service.proxy.core.inbound.handlers;

import cn.iinti.malenia2.service.base.trace.Recorder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Queue;

public abstract class AbstractFirstHttpHandler extends ChannelInboundHandlerAdapter {

    protected final Recorder recorder;
    protected final boolean isHttps;

    protected ChannelHandlerContext ctx = null;
    protected HttpRequest httpRequest;

    protected Queue<HttpObject> httpObjects;

    public AbstractFirstHttpHandler(Recorder recorder, boolean isHttps) {
        this.isHttps = isHttps;
        this.recorder = recorder;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
        this.ctx = ctx;
        if (!initRequest(msg)) {
            return;
        }
        afterInitRequest();
    }

    abstract void afterInitRequest();

    private boolean initRequest(Object msg) {
        if (msg instanceof HttpRequest) {
            // 客户端重新发起鉴权，所以之前的请求就需要清空
            releaseHttpObjects();
            httpRequest = (HttpRequest) msg;
            httpObjects = new ArrayDeque<>();
            recorder.recordEvent(() -> "http request");
            if (!isHttps) {
                httpObjects.add(httpRequest);
            }
        } else if (msg instanceof HttpObject) {
            // 会有 DefaultLastHttpContent , DefaultHttpContent 进来
            // 即使我们设置了不允许读，所以我们吧已经解析到内存的数据保存下，
            // 等到我们卸载当然handler的时候一并写到上游
            httpObjects.add((HttpObject) msg);
            recorder.recordEvent(() -> "httpObject:" + msg.getClass().getName());
            return false;
        } else {
            recorder.recordEvent(() -> "not handle http message:" + msg);
            ReferenceCountUtil.release(msg);
            ctx.close();
            return false;
        }
        recorder.recordEvent(() -> "Received raw request from ip:" + ctx.channel().remoteAddress() + " local:" + ctx.channel().localAddress() + ": request: " + StringUtils.left(String.valueOf(httpRequest), 128));
        return true;
    }


    protected void releaseHttpObjects() {
        if (httpObjects != null) {
            for (HttpObject httpObject : httpObjects) {
                ReferenceCountUtil.release(httpObject);
            }
        }
        httpObjects = null;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (httpObjects != null) {
            HttpObject b;
            while ((b = httpObjects.poll()) != null) {
                ctx.fireChannelRead(b);
            }
        }
        ctx.flush();
        httpObjects = null;
    }
}