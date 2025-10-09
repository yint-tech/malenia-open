package cn.iinti.malenia2.entity;

import cn.iinti.malenia2.api.ip.resource.CountStatus;
import cn.iinti.malenia2.api.ip.resource.ProxyIp;
import cn.iinti.malenia2.service.proxy.core.outbound.ActiveProxyIp;
import cn.iinti.malenia2.service.proxy.core.outbound.downloader.DownloadProxyIp;
import com.maxmind.geoip2.model.CityResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;

import javax.annotation.Nullable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(name = "ip资源", description = "IP池中的ip资源")
public class VoIpItem extends ProxyIp {

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


    /**
     * 是否在线
     */
    private boolean online;

    /**
     * 总使用数量
     */
    private int totalCount;
    /**
     * 首次使用时间戳
     */
    private long fistActive;

    /**
     * 总运行流量
     */
    private long totalFlow;

    /**
     * 连接缓存池中资源数量
     */
    private long cacheConnSize;


    public static VoIpItem fromDownloadIp(DownloadProxyIp downloadProxyIp) {
        VoIpItem voIpItem = new VoIpItem();
        BeanUtils.copyProperties(downloadProxyIp.getProxyIp(), voIpItem);

        voIpItem.setTestCost(downloadProxyIp.getTestCost());
        voIpItem.setEnQueueTime(downloadProxyIp.getEnQueueTime());
        voIpItem.setCityResponse(downloadProxyIp.getCityResponse());
        voIpItem.setOnline(false);
        return voIpItem;
    }

    public static VoIpItem fromActivityIp(ActiveProxyIp activeProxyIp) {
        VoIpItem voIpItem = fromDownloadIp(activeProxyIp.getDownloadProxyIp());
        voIpItem.setOnline(true);
        CountStatus countStatus = activeProxyIp.getCountStatus();
        voIpItem.setTotalCount(countStatus.getTotalCount());
        voIpItem.setFistActive(countStatus.getFistActive());
        voIpItem.setTotalFlow(countStatus.getTotalFlow());

        voIpItem.setCacheConnSize(activeProxyIp.cacheConnSize());

        return voIpItem;
    }
}
