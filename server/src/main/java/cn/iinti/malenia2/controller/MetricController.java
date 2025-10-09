package cn.iinti.malenia2.controller;

import cn.iinti.malenia2.MaleniaMain;
import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.entity.CommonRes;
import cn.iinti.malenia2.entity.metric.Metric;
import cn.iinti.malenia2.entity.metric.MetricDay;
import cn.iinti.malenia2.entity.metric.MetricTag;
import cn.iinti.malenia2.mapper.metric.MetricTagMapper;
import cn.iinti.malenia2.service.base.BroadcastService;
import cn.iinti.malenia2.service.base.metric.*;
import cn.iinti.malenia2.service.base.metric.mql.MQL;
import cn.iinti.malenia2.system.LoginRequired;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "监控指标相关")
@RequestMapping(BuildConfig.restfulApiPrefix + "/metric")
@Validated
@Slf4j
public class MetricController {

    @Resource
    private MetricService metricService;

    @Resource
    private MetricTagService metricTagService;


    @Resource
    private MetricTagMapper metricTagMapper;

    @Data
    public static class MetricQueryRequest {
        @NotBlank
        private String name;
        private Map<String, String> query;
        @NotNull
        private MetricEnums.MetricAccuracy accuracy;
    }

    @Operation(summary = "查询指标,可以指定tag")
    @PostMapping("/queryMetric")
    @LoginRequired
    public CommonRes<List<MetricVo>> queryMetric(@RequestBody @Validated MetricQueryRequest metricQueryRequest) {
        return CommonRes.success(metricService.queryMetric(metricQueryRequest.name, metricQueryRequest.query, metricQueryRequest.accuracy));
    }

    @Operation(summary = "查询指标,使用mql语言查询")
    @RequestMapping(value = "/mqlQuery", method = {RequestMethod.GET, RequestMethod.POST})
    @LoginRequired
    public CommonRes<EChart4MQL> mqlQuery(@NotBlank String mqlScript, @NotNull MetricEnums.MetricAccuracy accuracy) {
        return CommonRes.call(() -> EChart4MQL.fromMQLResult(MQL.compile(mqlScript).run(accuracy, metricService)));
    }

    @Operation(summary = "指标列表")
    @GetMapping("/metricNames")
    @LoginRequired
    public CommonRes<List<String>> metricNames() {
        return CommonRes.success(metricTagService.metricNames());
    }


    @Operation(summary = "指标Tag详情")
    @GetMapping("/metricTag")
    @LoginRequired
    public CommonRes<MetricTag> queryMetricTag(@NotBlank String metricName) {
        return CommonRes.ofPresent(metricTagService.fromKey(metricName));
    }

    @Operation(summary = "所有的指标详情")
    @GetMapping("/allMetricConfig")
    @LoginRequired
    public CommonRes<List<MetricTag>> allMetricConfig() {
        return CommonRes.success(metricTagService.tagList());
    }

    @Operation(summary = "删除一个指标")
    @GetMapping("/deleteMetric")
    @LoginRequired
    public CommonRes<String> deleteMetric(@NotBlank String metricName) {
        MaleniaMain.getShardThread().post(() -> {
            metricService.eachDao(mapper -> mapper.delete(new QueryWrapper<Metric>().eq(MetricDay.NAME, metricName)));
            metricTagMapper.delete(new QueryWrapper<MetricTag>().eq(MetricTag.NAME, metricName));
            BroadcastService.triggerEvent(BroadcastService.Topic.METRIC_TAG);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
            metricService.eachDao(mapper -> mapper.delete(new QueryWrapper<Metric>().eq(MetricDay.NAME, metricName)));
        });
        return CommonRes.success("ok");
    }

}
