package cn.iinti.malenia2.service.base.env;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.entity.CommonRes;
import cn.iinti.malenia2.service.base.config.Configs;
import cn.iinti.malenia2.utils.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class Environment {
    public static String APPLICATION_PROPERTIES = "application.properties";

    public static int tomcatPort;
    public static final boolean isLocalDebug =
            BooleanUtils.isTrue(Boolean.valueOf(Configs.getConfig("env.localDebug", "false")));
    public static final boolean isDemoSite =
            BooleanUtils.isTrue(Boolean.valueOf(Configs.getConfig("env.demoSite", "false")));

    public static final String mockProxySpace = Configs.getConfig("env.mockProxySpace", "");

    public static final String personalAppcode = Configs.getConfig("env.certification.personal.appcode", "");
    public static final String enterpriseAppcode = Configs.getConfig("env.certification.enterprise.appcode", "");
    public static final boolean supportCertification = StringUtils.isNoneBlank(personalAppcode, enterpriseAppcode);

    public static final String aliPayKeyPublic = Configs.getConfig("env.pay.ali.keyPublic", "");
    public static final String aliPayKeyPrivate = Configs.getConfig("env.pay.ali.keyPrivate", "");
    public static final String aliPayAppId = Configs.getConfig("env.pay.ali.appId", "");
    public static final boolean supportAlipay = StringUtils.isNoneBlank(aliPayAppId, aliPayKeyPublic, aliPayKeyPrivate);

    public static final String wechatPayMerchantId = Configs.getConfig("env.pay.wechat.merchantId", "");
    public static final String wechatPayKeyPrivate = Configs.getConfig("env.pay.wechat.keyPrivate", "");
    public static final String wechatPayMerchantSerialNumber = Configs.getConfig("env.pay.wechat.merchantSerialNumber", "");
    public static final String wechatPayApiV3Key = Configs.getConfig("env.pay.wechat.apiV3Key", "");
    public static final String wechatPayAppId = Configs.getConfig("env.pay.wechat.appId", "");

    public static final boolean supportWechatPay = StringUtils.isNoneBlank(wechatPayMerchantId, wechatPayKeyPrivate,
            wechatPayMerchantSerialNumber, wechatPayApiV3Key, wechatPayAppId);


    public static final File runtimeClassPathDir = resolveClassPathDir();

    public static final boolean isIdeDevelopment = !runtimeClassPathDir.getName().equals("conf");

    public static final File storageRoot = CommonUtils.forceMkdir(resolveStorageRoot());


    public static CommonRes<JSONObject> buildInfo() {
        return CommonRes.success(new JSONObject()
                .fluentPut("buildInfo",
                        new JSONObject()
                                .fluentPut("versionCode", BuildConfig.versionCode)
                                .fluentPut("versionName", BuildConfig.versionName)
                                .fluentPut("buildTime", BuildConfig.buildTime)
                                .fluentPut("buildUser", BuildConfig.buildUser)
                                .fluentPut("gitId", GitProperties.GIT_ID.value)
                                .fluentPut("supportCertification", supportCertification)
                                .fluentPut("supportAlipay", supportAlipay)
                                .fluentPut("supportWechatPay", supportWechatPay)
                ).fluentPut("env",
                        new JSONObject()
                                .fluentPut("demoSite", isDemoSite)
                                .fluentPut("debug", isLocalDebug)
                )

        );
    }

    @Getter
    private static ApplicationContext app;


    public static void setupApp(WebServerInitializedEvent event) {
        app = event.getApplicationContext();
        tomcatPort = event.getWebServer().getPort();
    }

    @SneakyThrows
    public static void upgradeIfNeed(DataSource dataSource) {
        upgradeRuleHolders.sort(Comparator.comparingInt(o -> o.fromVersionCode));
        doDbUpGradeTask(dataSource);

        if (isIdeDevelopment) {
            // 本地代码执行模式，认为一定时最新版本，不需要执行升级代码
            return;
        }
        doLocalUpGradeTask(new File(runtimeClassPathDir, "versionCode.txt"));
        System.out.println("app: " + BuildConfig.appName + " version:(" + BuildConfig.versionCode + ":" + BuildConfig.versionName + ") buildTime:" + BuildConfig.buildTime);
    }

    @SuppressWarnings("all")
    private static final String DB_VERSION_SQL =
            "select config_value from sys_config where config_key='_malenia_framework_version' and config_comment='_malenia_framework'";

    @SuppressWarnings("all")
    private static final String UPDATE_DB_VERSION_SQL =
            "insert into sys_config (`config_comment`,`config_key`,`config_value`) values ('_malenia_framework','_malenia_framework_version','" + BuildConfig.versionCode + "') " +
                    "on duplicate key update `config_value`='" + BuildConfig.versionCode + "'";

    private static void doDbUpGradeTask(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // fetch preVersion
            try (Statement statement = conn.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(DB_VERSION_SQL)) {
                    if (resultSet.next()) {
                        int preVersionCode = Integer.parseInt(resultSet.getString(1));
                        for (UpgradeRuleHolder upgradeRuleHolder : upgradeRuleHolders) {
                            if (upgradeRuleHolder.fromVersionCode < preVersionCode) {
                                continue;
                            }
                            System.out.println("db upgrade app from: " + upgradeRuleHolder.fromVersionCode + " to: " + upgradeRuleHolder.toVersionCode);
                            upgradeRuleHolder.upgradeHandler.doDbUpgrade(dataSource);
                            preVersionCode = upgradeRuleHolder.toVersionCode;
                        }
                    }
                }
            }

            // flush now version
            try (Statement statement = conn.createStatement()) {
                statement.execute(UPDATE_DB_VERSION_SQL);
            }
        }
    }

    private static void doLocalUpGradeTask(File versionCodeFile) throws IOException {
        if (versionCodeFile.exists()) {
            int preVersionCode = Integer.parseInt(FileUtils.readFileToString(versionCodeFile, StandardCharsets.UTF_8));

            for (UpgradeRuleHolder upgradeRuleHolder : upgradeRuleHolders) {
                if (upgradeRuleHolder.fromVersionCode < preVersionCode) {
                    continue;
                }
                System.out.println("local upgrade app from: " + upgradeRuleHolder.fromVersionCode + " to: " + upgradeRuleHolder.toVersionCode);
                upgradeRuleHolder.upgradeHandler.doLocalUpgrade();
                preVersionCode = upgradeRuleHolder.toVersionCode;
            }
        }


        FileUtils.write(versionCodeFile, String.valueOf(BuildConfig.versionCode), StandardCharsets.UTF_8);
    }

    private static File resolveClassPathDir() {
        URL configURL = Environment.class.getClassLoader().getResource(Environment.APPLICATION_PROPERTIES);
        if (configURL != null && configURL.getProtocol().equals("file")) {
            File classPathDir = new File(configURL.getFile()).getParentFile();
            String absolutePath = classPathDir.getAbsolutePath();
            if (absolutePath.endsWith("target/classes") // with maven
                    || absolutePath.endsWith("build/resources/main") // with gradle
                    || absolutePath.endsWith("conf") // with distribution
                    || absolutePath.endsWith("build\\resources\\main")//with gradle and Windows
            ) {
                return classPathDir;
            }
        }
        throw new IllegalStateException("can not resolve env: " + configURL);
    }

    private static File resolveStorageRoot() {
        if (isIdeDevelopment) {
            return new File(FileUtils.getUserDirectory(), BuildConfig.appName);
        }
        return new File(runtimeClassPathDir.getParent(), "data");
    }

    private static final List<UpgradeRuleHolder> upgradeRuleHolders = new ArrayList<>();

    @SuppressWarnings("all")
    private static void registerUpgradeTask(int fromVersionCode, int toVersionCode, UpgradeHandler upgradeHandler) {
        upgradeRuleHolders.add(new UpgradeRuleHolder(fromVersionCode, toVersionCode, upgradeHandler));
    }

    static {
        registerUpgradeTask(2, 3, new SQLExecuteUpgradeHandler("upgrade_2_3.sql"));
        registerUpgradeTask(3, 4, new SQLExecuteUpgradeHandler("upgrade_3_4.sql"));
    }

    @AllArgsConstructor
    private static class UpgradeRuleHolder {
        private int fromVersionCode;
        private int toVersionCode;
        private UpgradeHandler upgradeHandler;
    }


    public static void registerShutdownHook(Runnable runnable) {
        ShutdownHook.registerShutdownHook(runnable);
    }


    public static int prepareShutdown() {
        return ShutdownHook.prepareShutdown();
    }
}
