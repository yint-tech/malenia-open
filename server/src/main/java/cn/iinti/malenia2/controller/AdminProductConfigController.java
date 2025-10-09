package cn.iinti.malenia2.controller;


import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.entity.*;
import cn.iinti.malenia2.mapper.IpSourceMapper;
import cn.iinti.malenia2.mapper.ProductMapper;
import cn.iinti.malenia2.mapper.ProductSourceMapper;
import cn.iinti.malenia2.mapper.RechargeRecordMapper;
import cn.iinti.malenia2.service.base.BroadcastService;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.proxy.core.outbound.handshark.Protocol;
import cn.iinti.malenia2.service.proxy.dbconfigs.DbConfigs;
import cn.iinti.malenia2.service.proxy.dbconfigs.WrapperIpSource;
import cn.iinti.malenia2.service.proxy.mock.MockProxyServer;
import cn.iinti.malenia2.service.proxy.utils.PortSpaceParser;
import cn.iinti.malenia2.system.LoginRequired;
import cn.iinti.malenia2.utils.ExcelUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * <p>
 * 代理产品，代理ip资源等后台基础信息配置，他应该全部给超级管理员使用
 * </p>
 *
 * @author virjar
 * @since 2023-10-31
 */
@RestController
@RequestMapping(BuildConfig.restfulApiPrefix + "/admin-config")
public class AdminProductConfigController {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ProductSourceMapper productSourceMapper;

    @Resource
    private IpSourceMapper ipSourceMapper;

    @Resource
    private RechargeRecordMapper rechargeRecordMapper;


    @Operation(summary = "列出支持的返回值模版（过期）")
    @LoginRequired(forAdmin = true)
    @Deprecated
    @GetMapping("/listEmbedIpResourceHandlers")
    public CommonRes<List<JSONObject>> listEmbedIpResourceHandlers() {
        return CommonRes.success(Collections.emptyList());
    }

    @Operation(summary = "新增/更新代理产品", description = "根据产品id是否存在判断更新或新增代理产品")
    @LoginRequired(forAdmin = true, alert = true)
    @PostMapping("/editProduct")
    public CommonRes<String> editProduct(@RequestBody @Validated Product product) {
        Set<Integer> listenPorts = DbConfigs.listenPorts(product.getProductId());
        listenPorts = Sets.newHashSet(listenPorts);
        // 端口不能使用http服务对应的端口，这会导致冲突
        listenPorts.add(Environment.tomcatPort);
        // 如果有embed server，那么不能使用embed server作为产品端口
        listenPorts.addAll(MockProxyServer.getRunningMockPorts());

        TreeSet<Integer> targetMappingPorts = PortSpaceParser.parsePortSpace(product.getMappingPortSpace());
        if (targetMappingPorts.isEmpty()) {
            return CommonRes.failed("portSpace value empty!!");
        }
        listenPorts.retainAll(targetMappingPorts);
        if (!listenPorts.isEmpty()) {
            return CommonRes.failed("port space conclusion: " + StringUtils.join(listenPorts));
        }

        product.setEnabled(true);


        Product exist = null;
        if (product.getId() != null || StringUtils.isNotBlank(product.getProductId())) {
            QueryWrapper<Product> wrapper = new QueryWrapper<>();
            if (product.getId() != null) {
                wrapper.eq(Product.ID, product.getId());
            }
            if (StringUtils.isNotBlank(product.getProductId())) {
                wrapper.eq(Product.PRODUCT_ID, product.getProductId());
            }
            exist = productMapper.selectOne(wrapper);
        }
        if (exist != null) {
            product.setId(exist.getId());
            product.setProductId(exist.getProductId());
        }
        if (StringUtils.isBlank(product.getProductId())) {
            product.setProductId(UUID.randomUUID().toString());
        }

        if (product.getId() == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        BroadcastService.triggerEvent(BroadcastService.Topic.PRODUCT);
        return CommonRes.success("ok");
    }

    @Operation(summary = "更改代理产品生效状态")
    @LoginRequired(forAdmin = true, alert = true)
    @GetMapping("/updateProductStatus")
    public CommonRes<String> updateProductStatus(String productId, boolean enabled) {
        Product one = productMapper.selectOne(new QueryWrapper<Product>().eq(Product.PRODUCT_ID, productId));
        if (one == null) {
            return CommonRes.failed("no record: " + productId);
        }
        if (one.getEnabled() == enabled) {
            return CommonRes.success("ok");
        }

        if (enabled) {
            TreeSet<Integer> targetListenPort = PortSpaceParser.parsePortSpace(one.getMappingPortSpace());
            Set<Integer> listenPorts = DbConfigs.listenPorts(productId);
            listenPorts.retainAll(targetListenPort);
            // 下线再上线之后，需要重新计算一下是否存在端口冲突
            if (!listenPorts.isEmpty()) {
                return CommonRes.failed("port space conclusion: " + StringUtils.join(listenPorts));
            }
        }

        productMapper.update(null, new UpdateWrapper<Product>()
                .eq(Product.ID, one.getId())
                .set(Product.ENABLED, enabled)
        );

        BroadcastService.triggerEvent(BroadcastService.Topic.PRODUCT);
        return CommonRes.success("ok");
    }


    @Operation(summary = "获取产品下的代理Ip资源")
    @LoginRequired(forAdmin = true)
    @GetMapping("/listProductIpSources")
    public CommonRes<List<ProductSource>> listProductSource(String productId) {
        return CommonRes.success(productSourceMapper.selectList(
                new QueryWrapper<ProductSource>()
                        .eq(ProductSource.PRODUCT_ID, productId)
        ));
    }

    @Operation(summary = "添加代理Ip资源到产品")
    @LoginRequired(forAdmin = true)
    @GetMapping("/addSourceToProduct")
    public CommonRes<ProductSource> addSourceToProduct(String sourceKey, String productId, int ratio) {
        if (ratio < 0) {
            ratio = 0;
        }
        if (ratio > 20) {
            ratio = 20;
        }
        if (StringUtils.isBlank(sourceKey)) {
            return CommonRes.failed("need set sourceKey");
        }
        if (StringUtils.isBlank(productId)) {
            return CommonRes.failed("need set productId");
        }

        Long count = ipSourceMapper.selectCount(new QueryWrapper<IpSource>().eq(IpSource.SOURCE_KEY, sourceKey));
        if (count <= 0) {
            return CommonRes.failed("sourceKey not exist");
        }
        if (productMapper.selectCount(new QueryWrapper<Product>().eq(Product.PRODUCT_ID, productId)) <= 0) {
            return CommonRes.failed("product not exist");
        }

        ProductSource productSource = productSourceMapper.selectOne(
                new QueryWrapper<ProductSource>()
                        .eq(ProductSource.SOURCE_KEY, sourceKey)
                        .eq(ProductSource.PRODUCT_ID, productId)
        );
        if (productSource != null) {
            productSource.setRatio(ratio);
            productSourceMapper.updateById(productSource);
            return CommonRes.success(productSource);
        }

        productSource = new ProductSource();
        productSource.setRatio(ratio);
        productSource.setProductId(productId);
        productSource.setSourceKey(sourceKey);
        productSourceMapper.insert(productSource);
        BroadcastService.triggerEvent(BroadcastService.Topic.PRODUCT);
        return CommonRes.success(productSource);
    }

    @Operation(summary = "产品启动代理服务器端口列表")
    @LoginRequired(forAdmin = true)
    @GetMapping("/productPorts")
    public CommonRes<List<Integer>> productPorts(@NotBlank String productId) {
        return CommonRes.success(new ArrayList<>(DbConfigs.productUsedPorts(productId)));
    }


    @Operation(summary = "全部代理Ip资源")
    @LoginRequired(forAdmin = true)
    @GetMapping("/getAllIpSources")
    public CommonRes<List<IpSource>> getAllIpSources() {
        // 对于管理员，所有数据都是可以查看的
        return CommonRes.success(ipSourceMapper.selectList(new QueryWrapper<>()));
    }

    @Operation(summary = "获取单个IP资源详情")
    @LoginRequired(forAdmin = true)
    @GetMapping("/getIpSourceDetail")
    public CommonRes<IpSource> getIpSourceDetail(@NotBlank String ipSourceKey) {
        return CommonRes.ofPresent(
                ipSourceMapper.selectOne(new QueryWrapper<IpSource>().eq(IpSource.SOURCE_KEY, ipSourceKey))
        );
    }

    @Operation(summary = "获取IP资源最近的错误记录")
    @LoginRequired(forAdmin = true)
    @GetMapping("/lastErrorRecords4IpSource")
    public CommonRes<String> lastErrorRecords4IpSource(@NotBlank String ipSourceKey) {
        return CommonRes.success(DbConfigs.getResourceLastErrorRecords(ipSourceKey));
    }


    @Operation(summary = "获取IP资源池ip列表")
    @LoginRequired(forAdmin = true)
    @GetMapping("/getResourceContentList")
    public CommonRes<List<VoIpItem>> getResourceContentList(@NotBlank String ipSourceKey) {
        return DbConfigs.getResourceContentList(ipSourceKey);
    }

    @Operation(summary = "新增/更新代理上游资源")
    @LoginRequired(forAdmin = true, alert = true)
    @PostMapping("/editIpSource")
    public CommonRes<IpSource> editIpSource(@RequestBody @Validated IpSource ipSource) {
        try {
            WrapperIpSource.resolveIpResourceHandler(ipSource.getLoadResourceHandler());
        } catch (Exception e) {
            return CommonRes.failed(e);
        }


        List<Protocol> protocols = WrapperIpSource.parseSupportProtocol(ipSource.getSupportProtocol());
        if (protocols.isEmpty()) {
            return CommonRes.failed("支持协议为空");
        }

        // sourceKey不能有空格
        ipSource.setSourceKey(ipSource.getSourceKey().trim());
        ipSource.setLoadUrl(StringUtils.trimToEmpty(ipSource.getLoadUrl()));

        IpSource existIpSource;
        if (ipSource.getId() != null) {
            existIpSource = ipSourceMapper.selectById(ipSource.getId());
        } else {
            existIpSource = ipSourceMapper.selectOne(
                    new QueryWrapper<IpSource>().eq(IpSource.SOURCE_KEY, ipSource.getSourceKey())
            );
        }
        if (existIpSource != null) {
            // sourceKey唯一约束判断
            if (!StringUtils.equals(existIpSource.getSourceKey(), ipSource.getSourceKey())) {
                return CommonRes.failed("ip source key can not be modify");
            }
            ipSource.setId(existIpSource.getId());
            ipSourceMapper.updateById(ipSource);
        } else {
            ipSourceMapper.insert(ipSource);
        }
        BroadcastService.triggerEvent(BroadcastService.Topic.PRODUCT);
        return CommonRes.success(ipSource);
    }

    @Operation(summary = "移除代理上游资源")
    @LoginRequired(forAdmin = true, alert = true)
    @GetMapping("/deleteIpSourceFromProduct")
    public CommonRes<Boolean> deleteIpSourceFromProduct(Long id) {
        ProductSource existIpSource = productSourceMapper.selectById(id);
        if (existIpSource == null) {
            return CommonRes.failed("id not found.");
        }
        productSourceMapper.deleteById(id);
        BroadcastService.triggerEvent(BroadcastService.Topic.PRODUCT);
        return CommonRes.success(true);
    }

    @Operation(summary = "充值记录列表")
    @LoginRequired(forAdmin = true)
    @GetMapping("/listRechargeRecords")
    public CommonRes<IPage<RechargeRecord>> listRechargeRecords(String user, int page, int pageSize,
                                                                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        if (page < 1) {
            page = 1;
        }
        QueryWrapper<RechargeRecord> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(user)) {
            queryWrapper = queryWrapper.eq(RechargeRecord.USER, user);
        }
        queryWrapper = queryWrapper.orderByDesc(RechargeRecord.CREATE_TIME);
        if (startTime != null) {
            queryWrapper = queryWrapper.ge(RechargeRecord.CREATE_TIME, startTime);
        }
        if (endTime != null) {
            queryWrapper = queryWrapper.le(RechargeRecord.CREATE_TIME, endTime);
        }

        return CommonRes.success(rechargeRecordMapper.selectPage(new Page<>(page, pageSize), queryWrapper));
    }


    @Operation(summary = "导出充值记录")
    @LoginRequired(forAdmin = true, alert = true)
    @GetMapping("/exportRechargeRecord")
    public void exportBill(@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                           HttpServletResponse response) {
        QueryWrapper<RechargeRecord> queryWrapper = new QueryWrapper<RechargeRecord>()
                .gt(Bill.CREATE_TIME, startTime)
                .lt(Bill.CREATE_TIME, endTime);
        ExcelUtils.setExcelExportResponseHeader(response,
                "malenia-recharge-record-" +
                        startTime.format(DateTimeFormatter.ISO_DATE_TIME) + "-" +
                        endTime.format(DateTimeFormatter.ISO_DATE_TIME) +
                        ".xls");
        try (OutputStream os = response.getOutputStream()) {
            ExcelUtils.doExport(RechargeRecord.class, rechargeRecordMapper, queryWrapper, os);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
