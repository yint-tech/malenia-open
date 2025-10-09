package cn.iinti.malenia2.service.proxy.core.inbound.handlers;


import cn.iinti.malenia2.service.base.trace.Recorder;
import cn.iinti.malenia2.service.proxy.utils.NettyUtil;
import io.netty.handler.codec.http.HttpResponseStatus;

public class NormalHttpPacketHandler extends AbstractFirstHttpHandler {

    public NormalHttpPacketHandler(Recorder recorder, boolean isHttps) {
        super(recorder, isHttps);
    }

    @Override
    void afterInitRequest() {
        NettyUtil.httpResponseText(ctx.channel(), HttpResponseStatus.NOT_IMPLEMENTED,
                """
                        you should http webserver port, but this is proxy server port
                        """
        );
    }
}
