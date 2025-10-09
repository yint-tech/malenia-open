package cn.iinti.malenia2;

import cn.iinti.malenia2.entity.type.JSONTypeHandler;
import cn.iinti.malenia2.service.base.BroadcastService;
import cn.iinti.malenia2.service.base.config.ConfigService;
import cn.iinti.malenia2.service.base.env.Environment;
import cn.iinti.malenia2.service.base.safethread.Looper;
import cn.iinti.malenia2.service.proxy.NettyAllInOneServer;
import cn.iinti.malenia2.service.proxy.mock.MockProxyServer;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.util.IOUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import proguard.annotation.Keep;

import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

@SpringBootApplication
@EnableAspectJAutoProxy
@MapperScan("cn.iinti.malenia2.mapper")
@EnableScheduling
@Configuration
@Keep
public class MaleniaMain implements ApplicationListener<WebServerInitializedEvent>, CommandLineRunner {

    @Getter
    private static final Looper shardThread = new Looper("ShardThread").startLoop();


    @Resource
    private DataSource dataSource;

    @Resource
    private ConfigService configService;

    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void reloadConfig() {
        configService.reloadConfig();
    }

    @PostConstruct
    public void init() {
        // 第一步，在涉及到版本升级的场景下，调用版本管理器，进行升级操作，版本升级操作主要是对数据的表结构进行修改
        try {
            Environment.upgradeIfNeed(dataSource);
        } catch (Throwable throwable) {
            System.out.println("upgrade failed,please contact iint business support");
            throwable.printStackTrace(System.err);
            System.exit(1);
        }
        // 配置加载和刷新，需要放到Main上面，这样配置加载将会在所有bean启动前初始化好，
        // 让业务模块在运行的时候就拿到数据库的配置项
        reloadConfig();
        BroadcastService.register(BroadcastService.Topic.CONFIG, this::reloadConfig);
    }

    @Override
    public void onApplicationEvent(@Nonnull WebServerInitializedEvent event) {
        Environment.setupApp(event);
    }

    @Keep
    public static void main(String[] args) {
        List<String> argList = Lists.newArrayList(args);
        argList.add("--env.versionCode=" + BuildConfig.versionCode);
        argList.add("--env.versionName=" + BuildConfig.versionName);

        // setup log dir
        boolean hasSetupLogDir = argList.stream().anyMatch(s -> s.contains("--LogbackDir"));
        if (!hasSetupLogDir) {
            File logBase = Environment.runtimeClassPathDir;
            if (logBase.getName().equals("conf")) {
                logBase = logBase.getParentFile();
            } else if (logBase.toString().endsWith("build/resources/main")) {
                logBase = logBase.getParentFile().getParentFile();
            }
            argList.add("--LogbackDir=" + new File(logBase, "logs").getAbsolutePath());
        }

        //减少对用户的打扰，各组件大概率不会需要配置的参数，直接硬编码到源码中
        springContextParamSetup(argList);

        InputStream stream = MaleniaMain.class.getClassLoader().getResourceAsStream("addition.txt");
        if (stream != null) {
            List<String> strings = Splitter.on(CharMatcher.breakingWhitespace())
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(IOUtils.toString(stream, StandardCharsets.UTF_8));

            for (String str : strings) {
                if (!argList.contains(str)) {
                    argList.add(str);
                }
            }
        }
        runApp(argList, throwable -> {
            if (Environment.isIdeDevelopment && StringUtils.contains(throwable.getMessage(),
                    "Docker is not running")) {
                // 有一些时候用户在本地开发的时候,不想依赖docker，那么这个时候尝试禁用docker，直接使用本机配置尝试启动服务
                argList.add("--spring.docker.compose.enabled=false");
                throwable.printStackTrace(System.err);
                runApp(argList, defaultErrorHandle);
                return;
            }
            defaultErrorHandle.accept(throwable);
        });
    }


    private static void runApp(List<String> argList, Consumer<Throwable> errorHandle) {
        try {
            SpringApplication.run(MaleniaMain.class, argList.toArray(new String[]{}));
        } catch (Throwable throwable) {
            errorHandle.accept(throwable);
        }
    }

    private static final Consumer<Throwable> defaultErrorHandle = throwable -> {
        throwable.printStackTrace(System.err);
        // 如果启动失败，必须退出，否则docker的进程守护无法感知到服务启动失败
        System.exit(1);
    };

    private static void springContextParamSetup(List<String> argList) {
        argList.add("--spring.main.allow-circular-references=true");
        // setup docker for development
        if (Environment.isIdeDevelopment) {
            argList.add("--spring.docker.compose.file=classpath:develop-malenia/docker-compose-local.yaml");
            argList.add("--spring.docker.compose.lifecycle-management=start-only");
        }

        // setup metric config
        checkAddPram(argList, "spring.application.name", BuildConfig.appName);
        checkAddPram(argList, "management.endpoints.web.exposure.include", "*");
        checkAddPram(argList, "management.endpoints.web.base-path", BuildConfig.restfulApiPrefix + "/actuator");
        checkAddPram(argList, "management.metrics.tags.application", BuildConfig.appName);

        // setup mbp
        checkAddPram(argList, "mybatis-plus.configuration.default-enum-type-handler", EnumOrdinalTypeHandler.class.getName());
        checkAddPram(argList, "mybatis-plus.type-handlers-package", JSONTypeHandler.class.getPackageName());

        // setup compression
        checkAddPram(argList, "server.compression.enabled", "true");
        checkAddPram(argList, "server.compression.mime-types",
                "application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css"
        );
        checkAddPram(argList, "server.compression.min-response-size", "10");
        checkAddPram(argList, "server.compression.excluded-user-agents", "gozilla,traviata");

        // file upload
        checkAddPram(argList, "spring.servlet.multipart.max-file-size", "100MB");
        checkAddPram(argList, "spring.servlet.multipart.max-request-size", "102MB");

        // setup jackson
        checkAddPram(argList, "spring.jackson.date-format", "yyyy-MM-dd HH:mm:ss");
        checkAddPram(argList, "spring.jackson.time-zone", "GMT+8");

        // jdbc driver, we only support mysql
        checkAddPram(argList, "spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
    }

    private static void checkAddPram(List<String> argList, String key, String value) {
        for (String exist : argList) {
            if (StringUtils.containsIgnoreCase(exist, key)) {
                return;
            }
        }
        argList.add("--" + key + "=" + value);
    }

    @Override
    @Keep
    public void run(String... args) {
        ApplicationContext app = Environment.getApp();

        // 启动代理核心服务
        app.getBean(NettyAllInOneServer.class).startup();

        // 启动mock代理服务
        MockProxyServer.startMockService();
    }
}
