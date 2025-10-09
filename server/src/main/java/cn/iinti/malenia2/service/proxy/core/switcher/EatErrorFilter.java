package cn.iinti.malenia2.service.proxy.core.switcher;

import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.core.outbound.handshark.Protocol;
import io.netty.channel.Channel;

public class EatErrorFilter implements UpstreamHandSharkCallback {
    private final UpstreamHandSharkCallback delegate;
    private final Recorder recorder;

    public EatErrorFilter(UpstreamHandSharkCallback delegate, Recorder recorder) {
        this.delegate = delegate;
        this.recorder = recorder;
    }

    private volatile boolean hasSendErrorMsg = false;

    @Override
    public void onHandSharkFinished(Channel upstreamChannel, Protocol outboundProtocol) {
        if (hasSendErrorMsg) {
            recorder.recordEvent(() -> "hasSendErrorMsg ignore callback onHandSharkFinished");
            return;
        }
        delegate.onHandSharkFinished(upstreamChannel,outboundProtocol);
    }

    @Override
    public void onHandSharkError(Throwable e) {
        if (hasSendErrorMsg) {
            recorder.recordEvent(() -> "duplicate callback on hand shark");
            return;
        }
        hasSendErrorMsg = true;
        delegate.onHandSharkError(e);
    }
}