import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

var versionCode by extra(4)
var versionName by extra("2.2")
var applicationId by extra("cn.iinti.malenia2")
var docPath by extra("malenia-doc")
var userLoginTokenKey by extra("Malenia-Token")
var restfulApiPrefix by extra("/malenia-api")
var appName by extra("malenia")
var enableAmsNotice by extra(false)

var buildTime: String by extra(
    LocalDateTime.now().format(
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd_HH:mm:ss",
            java.util.Locale.CHINA
        )
    )
)

var buildUser: String by extra {
    var user = System.getenv("USER")
    if (user == null || user.isEmpty()) {
        user = System.getenv("USERNAME")
    }
    user
}

// 前端工具链相关
val yarnVersionStr by extra("4.1.1")
val nodeVersionStr by extra("22.20.0")
var nodeDistMirror by extra("https://mirrors.ustc.edu.cn/node")

// 因体产品开关
var yIntProject by extra(true)