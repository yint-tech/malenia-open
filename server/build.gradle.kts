import org.apache.hc.core5.function.Supplier
import org.apache.tools.ant.filters.FixCrLfFilter
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.internal.plugins.DefaultJavaAppStartScriptGenerationDetails
import org.hidetake.groovy.ssh.connection.AllowAnyHosts
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.session.SessionHandler
import java.security.MessageDigest
import java.util.*


/////////////////////////////////////// helper function start //////////////////////////////////////////
fun Properties.getConfig(key: String, defaultValue: String): String {
    val value = getProperty(key)
    if (value == null || value.isBlank()) {
        return defaultValue
    }
    return value
}

fun String.ensureSlash(): String {
    return if (endsWith("/")) {
        this
    } else {
        "$this/"
    }
}

val defaultIdentifier: String = File(System.getProperty("user.home"), ".ssh/id_rsa").absolutePath
fun Properties.shellParam(prefix: String): MutableMap<String, Any> {
    val deployRemoteUser = getConfig("${prefix}.user", "root")
    val connectionParam = mutableMapOf(
        "user" to deployRemoteUser,
        "knownHosts" to AllowAnyHosts.instance,
    )
    getProperty("${prefix}.password").apply {
        if (this == null) {
            val identity = getConfig("${prefix}.identity", defaultIdentifier)
            connectionParam["identity"] = File(identity)
        } else {
            connectionParam["password"] = this
        }
    }
    return connectionParam
}

////////////////////////////////// helper function end /////////////////////////////////////

plugins {
    java
    id("com.github.gmazzo.buildconfig") version "5.6.7"
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
    id("com.gorylenko.gradle-git-properties") version "2.4.2"
    id("org.hidetake.ssh") version "2.11.2"
    application
}

gitProperties {
    failOnNoGitDirectory = false
    extProperty = "gitProps"
}

val applicationId: String by rootProject.extra
var versionCode: Int by rootProject.extra
var versionName: String by rootProject.extra
var buildTime: String by rootProject.extra
var buildUser: String by rootProject.extra
var docPath: String by rootProject.extra
var userLoginTokenKey: String by rootProject.extra
var restfulApiPrefix: String by rootProject.extra
var appName: String by rootProject.extra
val internalAPIKey = "some_key_$appName".run {
    // 这里不能使用随机数，否则每次编译不一样，会导致startup脚本在发布后重启时无法调用停机接口
    // 那么对于商业项目，此处需要给每个产品配置一个密码，避免互联网用户猜想得到密钥
    // 当然此密钥将不会引起业务数据泄漏，安全影响也不是特别大
    val md = MessageDigest.getInstance("MD5")
    val messageDigest = md.digest(toByteArray())
    messageDigest.joinToString("") { byte -> "%02x".format(byte) }
        .substring(0..10)
}

var yIntProject: Boolean by rootProject.extra

group = applicationId
version = versionName

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.6")
    testImplementation("com.baomidou:mybatis-plus-generator:3.5.3.2")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    compileOnly("net.sf.proguard:proguard-annotations:6.2.2")

    testImplementation("org.apache.velocity:velocity-engine-core:2.3")
    implementation("mysql:mysql-connector-java:8.0.28")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("org.slf4j:jcl-over-slf4j:1.7.30")
    implementation("org.slf4j:log4j-over-slf4j:1.7.30")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.10.0")
    implementation("com.alibaba:fastjson:1.2.79")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    implementation(project(":malenia-api"))
    compileOnly("net.sf.proguard:proguard-annotations:6.2.2")
    implementation("io.netty:netty-codec-http")
    implementation("io.netty:netty-codec-socks")
    implementation("io.netty:netty-tcnative:1.1.33.Fork26")
    implementation("org.bouncycastle:bcprov-jdk15on:1.55")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.55")
    implementation("com.maxmind.geoip2:geoip2:4.2.0")
    implementation("ch.hsr:geohash:1.4.0")
    implementation("org.apache.poi:poi-ooxml:3.11")
    implementation("com.alipay.sdk:alipay-sdk-java:4.39.27.ALL")
    implementation("com.github.wechatpay-apiv3:wechatpay-java:0.2.12")
}

tasks.test {
    useJUnitPlatform()
}

// https://stackoverflow.com/questions/35427830/gradle-how-to-create-distzip-without-parent-directory
// reset parent
distributions.main {
    contents.into("/")
}



application {
    mainClass = "cn.iinti.malenia2.MaleniaMain"
    applicationName = "MaleniaMain"
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=utf-8", "-Duser.timezone=GMT+08", "-XX:-OmitStackTraceInFastThrow"
    )
    // docker compose 使用文件夹名称来命名，未了避免冲突，这里将文件夹带上项目名称
    applicationDistribution.from("${projectDir}/src/main/resources/develop-malenia") {
        include("ddl.sql")
        into("conf/")
    }

    applicationDistribution.from("${projectDir}/src/main/resources/application.properties") {
        into("conf/")
    }
    applicationDistribution.from("${projectDir}/assets/startup.sh") {
        into("bin/")
        eachFile {
            filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
            filter<ReplaceTokens>("tokens" to mapOf("internalAPIKey" to internalAPIKey))
            permissions {
                // 在windows上面构建代码的话，权限和回车可能不对，这里修复一下
                listOf<ConfigurableUserClassFilePermissions>(user, group, other).forEach {
                    it.execute = true
                }
            }
        }
    }

    applicationDistribution.from("${rootProject.projectDir}/frontend/build") {
        into("conf/static/")
    }
    applicationDistribution.from("${rootProject.projectDir}/doc/src/.vuepress/dist") {
        into("conf/static/malenia-doc")
    }

    applicationDistribution.from("${projectDir}/build/mitm-dsl-project.zip") {
        into("conf/static/build-in-res/")
    }

    // https://stackoverflow.com/questions/35427830/gradle-how-to-create-distzip-without-parent-directory
    applicationDistribution.into("/")
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("malenia-server")
}

tasks.getByPath("startScripts").doFirst {
    (this as CreateStartScripts).apply {
        fun wrapScriptGenerator(delegate: ScriptGenerator): ScriptGenerator {
            return ScriptGenerator { details, destination ->
                // 增加一个conf的目录，作为最终目标的classPath，在最终发布的时候，我们需要植入静态资源
                (details as DefaultJavaAppStartScriptGenerationDetails)
                    .apply {
                        classpath.removeIf {
                            // fix, why this fuck dependency present?
                            it.contains("spring-boot-docker-compose")
                        }
                        classpath.add(0, "conf")
                    }
                delegate.generateScript(details, destination)
            }
        }
        unixStartScriptGenerator = wrapScriptGenerator(unixStartScriptGenerator)
        windowsStartScriptGenerator = wrapScriptGenerator(windowsStartScriptGenerator)
    }
}

var zipDSLProjectCode = tasks.register<Zip>("zipDSLProjectCode") {
    archiveFileName.set("mitm-dsl-project.zip")
    destinationDirectory.set(projectDir.resolve("build/"))
    from(rootProject.projectDir.resolve("malenia-api")) {
        exclude(".gradle/**", "**/build/**")
        eachFile {
            if (name == "settings.gradle.kts.txt") {
                name = "settings.gradle.kts"
            }
        }
    }
}


buildConfig {
    useJavaOutput()
    buildConfig {
        packageName = applicationId
        buildConfigField("versionCode", versionCode)
        buildConfigField("versionName", versionName)
        buildConfigField("buildTime", buildTime)
        buildConfigField("buildUser", buildUser)
        buildConfigField("docPath", docPath)
        buildConfigField("userLoginTokenKey", userLoginTokenKey)
        buildConfigField("restfulApiPrefix", restfulApiPrefix)
        buildConfigField("appName", appName)
        buildConfigField("internalAPIKey", internalAPIKey)
    }
}


fun configDeployTask4Preset(presetFile: File, outputZipFile: Supplier<File>) {
    val config = Properties().apply {
        load(presetFile.inputStream())
    }

    config.getProperty("deploy.host")?.trim()?.split(',')?.let { deployServerList ->
        val name = presetFile.name
        val preset = name.subSequence("app_".length, name.lastIndexOf("."))
        val deployPath = config.getConfig("deploy.workdir", "/opt/malenia/").ensureSlash()
        val connParam = config.shellParam("deploy")

        config.getProperty("deploy.jump.host").apply {
            if (!this.isNullOrBlank()) {
                val host = this
                // 支持通过跳板机进行发布
                config.shellParam("deploy.jump").apply {
                    this["host"] = host
                    connParam["gateway"] = Remote(this)
                }
            }
        }

        tasks.register("deploy-${preset}") {
            group = "deploy"
            dependsOn(tasks.distZip)
            val remoteList = deployServerList.map {
                connParam["host"] = it
                Remote(connParam)
            }
            doLast {
                val copyZipCmd = hashMapOf(
                    "from" to outputZipFile.get(), "into" to deployPath
                )
                val copyPresetCmd = hashMapOf(
                    "from" to presetFile,
                    "into" to "${deployPath}conf/application.properties"
                )

                val zipFileInServer = "${deployPath}${outputZipFile.get().name}"

                remoteList.forEach {
                    ssh.run(delegateClosureOf<RunHandler> {
                        println("begin deploy server:$it")
                        session(it, delegateClosureOf<SessionHandler> {
                            execute("if [[ ! -d $deployPath ]] ; then mkdir  $deployPath ; fi")
                            put(copyZipCmd)
                            execute("unzip -q -o -d $deployPath $zipFileInServer")
                            put(copyPresetCmd)
                            execute("${deployPath}bin/startup.sh")
                        })
                    })
                }
            }
        }
    }
}

File(rootProject.projectDir, "deploy").listFiles { _, name ->
    name.startsWith("app_") && name.endsWith(".properties")
}?.apply {
    if (isEmpty()) {
        return@apply
    }

    var outputZipFile: File? = null
    val dipZipTask = tasks.distZip.get()

    dipZipTask.outputs.upToDateWhen { false }
    dipZipTask.doLast {
        outputZipFile = outputs.files.singleFile
    }

    forEach { presetFile ->
        configDeployTask4Preset(presetFile) {
            outputZipFile
        }
    }
}

// 请注意，下面代码是给因体产品使用的，对于开源项目，本功能应该是关闭的
// 开源用户打开开关也没有意义，因为他调用的是因体内部工具链
val yIntReleaseShell = File(rootProject.projectDir, "server/assets/iinti_release.sh")
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
if (yIntProject && !isWindows && yIntReleaseShell.exists()) {
    var outputZipFile: File? = null
    val dipZipTask = tasks.distZip.get()

    dipZipTask.outputs.upToDateWhen { false }
    dipZipTask.doLast {
        outputZipFile = outputs.files.singleFile
    }
    tasks.register("iinti-release") {
        group = "deploy"
        dependsOn(tasks.distZip)
        doLast {
            exec {
                commandLine(yIntReleaseShell, outputZipFile!!.absolutePath)
            }
        }
    }
}


afterEvaluate {
    tasks.startScripts {
        dependsOn(":frontend:yarnBuild")
        dependsOn(":doc:yarnBuild")
        dependsOn(zipDSLProjectCode)
    }
}


