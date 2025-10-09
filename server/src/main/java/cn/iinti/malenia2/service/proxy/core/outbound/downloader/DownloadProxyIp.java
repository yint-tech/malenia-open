package cn.iinti.malenia2.service.proxy.core.outbound.downloader;

import cn.iinti.malenia2.api.ip.resource.ProxyIp;
import com.alibaba.fastjson.JSONObject;
import com.maxmind.geoip2.model.CityResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

import javax.annotation.Nullable;

@Setter
@Getter
public class DownloadProxyIp {

    /**
     * 测试耗时
     */
    @Nullable
    private Long testCost;

    /**
     * 加入队列时间
     */
    private long enQueueTime;

    /**
     * 当前ip所在城市信息（包含国家-定位-isp等）
     */
    private CityResponse cityResponse;

    @Delegate
    private final ProxyIp proxyIp;

    public DownloadProxyIp(ProxyIp proxyIp) {
        this.proxyIp = proxyIp;
    }

    @Override
    public String toString() {
        return "DownloadProxyIp{" +
                "testCost=" + testCost +
                ", enQueueTime=" + enQueueTime +
                ", cityResponse=" + cityResponse +
                ", proxyIp=" + JSONObject.toJSONString(proxyIp) +
                '}';
    }
}
