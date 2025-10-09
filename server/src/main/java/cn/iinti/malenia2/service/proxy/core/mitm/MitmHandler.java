package cn.iinti.malenia2.service.proxy.core.mitm;

import cn.iinti.malenia2.api.mitm.data.ImmediatelyResponse;
import cn.iinti.malenia2.api.mitm.data.NameValuePair;
import cn.iinti.malenia2.api.mitm.interceptor.Interceptor;
import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.Session;
import cn.iinti.malenia2.service.proxy.core.mitm.api.ApiImpl;
import cn.iinti.malenia2.service.proxy.core.mitm.api.http.HttpRequestImpl;
import cn.iinti.malenia2.service.proxy.core.mitm.api.http.HttpResponseImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

public class MitmHandler extends ChannelDuplexHandler {

    private final boolean isHttps;
    private final Session session;
    private final Recorder recorder;
    private final Interceptor mitmInterceptor;

    private HttpRequestImpl requestImpl;

    public MitmHandler(boolean isHttps, Session session) {
        this.isHttps = isHttps;
        this.session = session;
        this.recorder = session.getRecorder();
        this.mitmInterceptor = session.getMitmInterceptor();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        recorder.recordEvent(() -> "enter mitm request handler: \n" + msg);
        if (!(msg instanceof FullHttpRequest)) {
            ctx.close();
            ReferenceCountUtil.release(msg);
            return;
        }

        FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
        if (fullHttpRequest.decoderResult().isFailure()) {
            recorder.recordEvent(() -> "http data decode error:" + fullHttpRequest.decoderResult());
            ctx.close();
            ReferenceCountUtil.release(msg);
            return;
        }
        requestImpl = new HttpRequestImpl(fullHttpRequest, recorder, isHttps);

        callInterceptor(() -> {
            try {
                String url = requestImpl.getUrl();
                recorder.recordEvent(() -> "intercept for url: " + url);
            } catch (Exception e) {
                recorder.recordEvent(() -> "gen url failed:" + fullHttpRequest, e);
            }
            try {
                mitmInterceptor.request(requestImpl);
            } catch (Throwable e) {
                recorder.recordEvent(() -> "interceptor request error", e);
            }
            ImmediatelyResponse immediatelyResponse = requestImpl.getImmediatelyResponse();

            if (immediatelyResponse != null) {
                recorder.recordEvent("prepare write new http request after interceptor");
                DefaultFullHttpResponse response = createImmediatelyResponse(immediatelyResponse);
                session.getInboundChannel()
                        .writeAndFlush(response)
                        .addListener((ChannelFutureListener) future -> {
                                    if (!future.isSuccess()) {
                                        recorder.recordEvent(() -> "write response failed", future.cause());
                                    }
                                }
                        );
                return;
            }

            requestImpl.flushContent();
            recorder.recordEvent(() -> "forward to upstream");
            ctx.fireChannelRead(requestImpl.getFullHttpRequest());
        }, () -> {
            recorder.recordEvent(() -> "mitm threadPool busy, cancel request intercept");
            ctx.fireChannelRead(msg);
        });
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ImmediatelyFullHttpResponse) {
            super.write(ctx, msg, promise);
            ReferenceCountUtil.release(requestImpl.getFullHttpRequest());
            return;
        }

        if (!(msg instanceof FullHttpResponse)) {
            ctx.close();
            recorder.recordEvent(() -> "error mitm response type: " + msg.getClass());
            ReferenceCountUtil.release(msg);
            ReferenceCountUtil.release(requestImpl.getFullHttpRequest());
            return;
        }
        recorder.recordEvent(() -> "enter mitm response handler: " + msg);

        FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;
        callInterceptor(() -> {
            HttpResponseImpl httpResponse = new HttpResponseImpl(fullHttpResponse, requestImpl);
            try {
                mitmInterceptor.response(httpResponse);
            } catch (Throwable throwable) {
                recorder.recordEvent(() -> "interceptor response error", throwable);
            } finally {
                ReferenceCountUtil.release(requestImpl.getFullHttpRequest());
            }
            recorder.recordEvent(() -> "response interceptor call finished,call flush content");
            httpResponse.flushContent();

            ImmediatelyResponse immediatelyResponse = requestImpl.getImmediatelyResponse();

            FullHttpResponse tempMsg = httpResponse.getFullHttpResponse();
            if (immediatelyResponse != null) {
                recorder.recordEvent("prepare write new http response after interceptor");
                ReferenceCountUtil.release(msg);
                tempMsg = createImmediatelyResponse(immediatelyResponse);
            }
            // 请注意，这里需要flush，因为mitm线程池延时了，上游应该是有flush的。但是因为我们切换线程池，导致flush失效了
            Object finalTempMsg = tempMsg;
            recorder.recordEvent(() -> "mitm response: " + finalTempMsg);

            ctx.writeAndFlush(tempMsg, promise);
        }, () -> {
            recorder.recordEvent(() -> "mitm threadPool busy, cancel response intercept");
            ctx.writeAndFlush(msg, promise);
        });

    }


    private ImmediatelyFullHttpResponse createImmediatelyResponse(ImmediatelyResponse immediatelyResponse) {
        recorder.recordEvent(() -> "response immediately...");
        ImmediatelyFullHttpResponse response;
        byte[] data = immediatelyResponse.getData();
        if (data == null) {
            response = new ImmediatelyFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(immediatelyResponse.getStatus())
            );
        } else {
            response = new ImmediatelyFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(immediatelyResponse.getStatus()),
                    Unpooled.wrappedBuffer(data)
            );
        }
        HttpHeaders headers = response.headers();
        for (NameValuePair nameValuePair : immediatelyResponse.getHeaders()) {
            headers.add(nameValuePair.getName(), nameValuePair.getValue());
        }
        // 长度是我们控制的
        headers.set(HttpHeaderNames.CONTENT_LENGTH, data == null ? "0" : String.valueOf(data.length));

        return response;
    }


    private void callInterceptor(Runnable interceptRunnable, Runnable cancelInterceptStep) {
        boolean status = MitmExecutor.submit(session.getWrapperUser().getUserName(),
                () -> {
                    try {
                        ApiImpl.setupLogger(recorder);
                        interceptRunnable.run();
                    } catch (Exception e) {
                        recorder.recordEvent(() -> "interceptor error", e);
                    } finally {
                        ApiImpl.cleanLogger();
                    }
                });
        if (status) {
            return;
        }

        if (mitmInterceptor.getConfig().isAbortWhenSubmitFailed()) {
            recorder.recordEvent("submit mitm task failed");
            session.getInboundChannel().close();
            return;
        }
        cancelInterceptStep.run();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        recorder.recordEvent(() -> "MITM handler error", cause);
    }

    private static class ImmediatelyFullHttpResponse extends DefaultFullHttpResponse {

        public ImmediatelyFullHttpResponse(HttpVersion version, HttpResponseStatus status) {
            super(version, status);
        }

        public ImmediatelyFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content) {
            super(version, status, content);
        }
    }

}
