package cn.iinti.malenia2.service.base.config;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.service.base.alert.EventScript;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.utils.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统设置,所有系统设置我们统一聚合在同一个文件中，避免默认值无法对齐
 */
public class Settings {

    public static JSONObject allSettingsVo() {
        JSONObject ret = new JSONObject();
        ret.put("normal", allSettings.stream().map(settingConfig ->
                        new JSONObject().fluentPut("key", settingConfig.getKey())
                                .fluentPut("value", settingConfig.valueVo())
                                .fluentPut("type", settingConfig.getSupplier().configType())
                                .fluentPut("desc", settingConfig.desc)
                                .fluentPut("detailDesc", settingConfig.detailDesc))
                .collect(Collectors.toList()));
        return ret;
    }

    private static final List<SettingConfig> allSettings = Lists.newArrayList();

    @SuppressWarnings("unused")
    private static Configs.BooleanConfigValue newBooleanConfig(String key, boolean defaultValue, String desc) {
        return newBooleanConfig(key, defaultValue, desc, desc);
    }

    private static Configs.BooleanConfigValue newBooleanConfig(String key, boolean defaultValue, String desc, String detailDesc) {
        Configs.BooleanConfigValue configValue = new Configs.BooleanConfigValue(key, defaultValue);
        allSettings.add(new SettingConfig(key, configValue, desc, detailDesc));
        return configValue;
    }

    @SuppressWarnings("unused")
    private static Configs.IntegerConfigValue newIntConfig(String key, int defaultValue, String desc) {
        return newIntConfig(key, defaultValue, desc, desc);
    }

    private static Configs.IntegerConfigValue newIntConfig(String key, int defaultValue, String desc, String detailDesc) {
        Configs.IntegerConfigValue configValue = new Configs.IntegerConfigValue(key, defaultValue);
        allSettings.add(new SettingConfig(key, configValue, desc, detailDesc));
        return configValue;
    }


    @SuppressWarnings("unused")
    private static Configs.StringConfigValue newStringConfig(String key, String defaultValue, String desc) {
        return newStringConfig(key, defaultValue, desc, desc);
    }

    private static Configs.StringConfigValue newStringConfig(String key, String defaultValue, String desc, String detailDesc) {
        Configs.StringConfigValue configValue = new Configs.StringConfigValue(key, defaultValue);
        allSettings.add(new SettingConfig(key, configValue, desc, detailDesc));
        return configValue;
    }

    @SuppressWarnings("unused")
    private static Configs.MultiLineStrConfigValue newMultilineStrConfig(String key, String defaultValue, String desc) {
        return newMultilineStrConfig(key, defaultValue, desc, desc);
    }

    private static Configs.MultiLineStrConfigValue newMultilineStrConfig(String key, String defaultValue, String desc, String detailDesc) {
        Configs.MultiLineStrConfigValue configValue = new Configs.MultiLineStrConfigValue(key, defaultValue);
        allSettings.add(new SettingConfig(key, configValue, desc, detailDesc));
        return configValue;
    }

    public static <T> Configs.ConfigValue<T> newCustomConfig(CustomConfigBuilder<T> builder) {
        Configs.ConfigValue<T> configValue = new Configs.ConfigValue<>(builder.configKey, builder.defaultValue) {
            @Override
            protected Configs.TransformFunc<T> transformer() {
                return builder.transformFunc;
            }

            @Override
            protected String configType() {
                return builder.configType;
            }
        };
        allSettings.add(new SettingConfig(builder.configKey, configValue, builder.desc, builder.detailDesc));
        return configValue;
    }

    @Builder
    public static class CustomConfigBuilder<T> {
        @NotBlank
        private String configKey;
        private T defaultValue;
        @NotBlank
        private String desc;
        private String detailDesc;
        private Configs.TransformFunc<T> transformFunc;
        private String configType;
    }

    @Getter
    @AllArgsConstructor
    public static class SettingConfig {
        private String key;
        private Configs.ConfigValue<?> supplier;
        private String desc;
        private String detailDesc;

        public Object valueVo() {
            if (supplier.value != null && ClassUtils.isPrimitiveOrWrapper(supplier.value.getClass())) {
                return supplier.value;
            }
            return supplier.sValue;
        }
    }


    public static final Configs.BooleanConfigValue allowRegisterUser = newBooleanConfig(
            BuildConfig.appName + ".user.allowRegister", false, "是否允许注册用户",
            "设置不允许注册新用户，则可以避免用户空白注册，规避系统安全机制不完善，让敏感数据通过注册泄漏"
    );

    public static final Configs.BooleanConfigValue blockSwagger = newBooleanConfig(
            BuildConfig.appName + ".user.blockSwagger", false, "拦截Swagger",
            "系统如果部署在公网，swagger将会展示接口信息，实例在政府机构内部部署等情况将会被安全扫组件判定为数据泄露"
    );

    public static final Configs.BooleanConfigValue blockActuator = newBooleanConfig(
            BuildConfig.appName + ".user.blockActuator", false, "拦截Actuator",
            "系统如果部署在公网，Actuator可以导出监控指标数据，实例在政府机构内部部署等情况将会被安全扫组件判定为数据泄露"
    );

    public static final Configs.StringConfigValue outIpTestUrl = newStringConfig(
            BuildConfig.appName + ".outIpTestUrl", "https://iinti.cn/conn/getPublicIp?scene=" + BuildConfig.appName,
            "出口ip探测URL", "计算当前服务器节点的出口IP，用于多节点部署在公网时多节点事件通讯"
    );


    public static final Configs.StringConfigValue systemNotice = newStringConfig(
            BuildConfig.appName + ".systemNotice", "",
            "系统通告信息", "在框架前端系统，将会在用户avatar推送消息"
    );

    public static final Configs.StringConfigValue docNotice = newStringConfig(
            BuildConfig.appName + ".docNotice", "",
            "文档首页通告信息", "在框架文档系统中，将会推送一段消息展示在文档中（此配置是html片段，故支持任意）"
    );

    public static Configs.ConfigValue<EventScript> eventNotifyScript = newCustomConfig(CustomConfigBuilder.<EventScript>builder()
            .configKey(BuildConfig.appName + ".eventNotifyScript")
            .transformFunc((value, type) -> {
                if (StringUtils.isNotBlank(value)) {
                    return EventScript.compileScript(value);
                }
                return null;

            })
            .desc("事件通知处理脚本")
            .detailDesc("内部事件，通过调用脚本的方式通知到外部系统")
            .configType("multiLine")
            .build()
    );

    public static Configs.IntegerConfigValue maxWhiteIpPerUser = newIntConfig(
            BuildConfig.appName + ".maxWhiteIpPerUser", 40, "每个用户最大的白名单数量"
    );

    public static Configs.IntegerConfigValue flowBillDuration = newIntConfig(
            BuildConfig.appName + ".flowBillDurationMinutes", 5,
            "流量对账时间间隔", "主要针对于按量付费用户的实时扣费,即根据预充值模型，对用户消费完成的流量进行扣费结算，直到用户余额扣完为止"
    );

    public static final Configs.BooleanConfigValue enableAccessRecord = newBooleanConfig(
            BuildConfig.appName + ".enableAccessRecord", true, "是否记录用户访问日志",
            "如果您需要对客户的代理使用情况进行审计，则系统可以记录用户使用代理的基本日志（小时级别）"
    );

    public static final Configs.BooleanConfigValue autoDisableNoneActiveOrder = newBooleanConfig(
            BuildConfig.appName + ".autoDisableNoneActiveOrder", true, "自动关闭不活跃订单",
            "对于IP售卖系统，用户使用系统后长期不再使用，此时为了减少系统运算负担，系统将会自动关闭订单，之后不再尝试为期对账和指标采集"
    );

    public static Configs.IntegerConfigValue maxAccessRecordSaveDays = newIntConfig(
            BuildConfig.appName + ".maxAccessRecordSaveDays", 365,
            "系统最大的访问日志保存时间", "系统可以记录详细的用户访问代理日志，此日志颗粒度在账户+网站纬度，内容聚合在小时级别，此日志可以用于合规审计"
    );


    public static Configs.IntegerConfigValue defaultBandwidthLimit = newIntConfig(
            BuildConfig.appName + ".defaultBandwidthLimit", 3,
            "用户带宽付费模式下，默认带宽限制"
    );


    /**
     * MITM报文大小，默认2^20 = 1M,请注意超过配置大小的情况下，转发请求会失败，请不要使用mitm处理大报文
     */
    public static final Configs.IntegerConfigValue mitmAggregateSize = newIntConfig(
            BuildConfig.appName + ".mitm.AggregateSize", 2 << 20, "MITM报文最大大小",
            "请注意超过配置大小的情况下，转发请求会失败，请不要使用mitm处理大报文"
    );

    public static final Configs.IntegerConfigValue mitmThreadPoolSize = newIntConfig(
            BuildConfig.appName + ".mitm.threadPoolSize", 5,
            "mitm工作线程池大小", "mitm过程为用户自定义脚本，这可能带来平台稳定性问题，故mitm约束线程池范围中");

    public static final Configs.StringConfigValue autoGenSSLCertificateHostList = newStringConfig(
            BuildConfig.appName + ".autoGenSSLCertificateHostList", "malenia.iinti.cn",
            "自签名ssl证书域名列表",
            "英文逗号分割，给需要通过ssl链接malenia使用代理服务的场景使用"
    );

    public static final Configs.BooleanConfigValue enableFloatIpSourceRatio = newBooleanConfig(
            BuildConfig.appName + ".enableFloatIpSourceRatio", false, "启用IP池间浮动流量比例",
            "请求根据配置按照流量比例分流到多个IP源，本功能则在设置流量比例基础上，根据IP源健康状态动态调整流量比例，实现故障IP资源自动熔断"
    );


    /**
     * 目前不可以使用接口内部类和接口常量，目前这可能和因体的基础工具链冲突
     */
    public static class Storage {
        public static final File root = Environment.storageRoot;

        // 本地存储方案资源目录，如果用户没有配置任何云存储方案，那么系统默认是使用本地存储方案
        public static final File localStorage = CommonUtils.forceMkdir(new File(root, "storage"));
    }
}
