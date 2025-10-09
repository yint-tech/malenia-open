package cn.iinti.malenia2.service.proxy.core.switcher;

import cn.iinti.malenia2.service.proxy.core.outbound.handshark.Protocol;
import io.netty.channel.Channel;

import javax.annotation.Nullable;

public interface UpstreamHandSharkCallback {
    void onHandSharkFinished(Channel upstreamChannel, @Nullable Protocol outboundProtocol);

    void onHandSharkError(Throwable e);
}