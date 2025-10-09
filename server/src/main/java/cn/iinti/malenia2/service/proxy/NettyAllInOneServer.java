package cn.iinti.malenia2.service.proxy;

import cn.iinti.malenia2.entity.IpSource;
import cn.iinti.malenia2.entity.MitmScript;
import cn.iinti.malenia2.entity.Product;
import cn.iinti.malenia2.entity.UserOrder;
import cn.iinti.malenia2.mapper.*;
import cn.iinti.malenia2.service.base.BroadcastService;
import cn.iinti.malenia2.service.proxy.dbconfigs.DbConfigs;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NettyAllInOneServer {


    @Resource
    private IpSourceMapper ipSourceMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ProductSourceMapper productSourceMapper;

    @Resource
    private UserOrderMapper userOrderMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private AuthWhiteIpMapper authWhiteIpMapper;

    @Resource
    private MitmScriptMapper mitmScriptMapper;

    public void startup() {
        reloadUserInfo();

        BroadcastService.register(BroadcastService.Topic.USER, this::reloadUserInfo);
        BroadcastService.register(BroadcastService.Topic.PRODUCT, this::reloadProductInfo);
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    private void reloadUserInfo() {
        DbConfigs.reloadUserConfig(
                userInfoMapper.selectList(new QueryWrapper<>()),
                userOrderMapper.selectList(new QueryWrapper<UserOrder>().eq(UserOrder.ENABLED, true)),
                authWhiteIpMapper.selectList(new QueryWrapper<>()),
                mitmScriptMapper.selectList(new QueryWrapper<MitmScript>().eq(MitmScript.ENABLED, true))
        );
    }

    /**
     * 系统启3s后，拉起代理服务,需要注意的是，系统刚刚启动马上就拉起代理服务没有意义<br>
     * 因为代理资源入库需要一定的时间
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 3 * 1000)
    private void reloadProductInfo() {
        DbConfigs.refreshProduct(
                productMapper.selectList(new QueryWrapper<Product>().eq(Product.ENABLED, true)),
                productSourceMapper.selectList(new QueryWrapper<>())
        );
        DbConfigs.reloadIpSource(
                ipSourceMapper.selectList(new QueryWrapper<IpSource>().eq(IpSource.ENABLED, true)));
    }
}
