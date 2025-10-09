rootProject.name = "malenia"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven {
            setUrl("https://maven.aliyun.com/repository/public")
        }
        maven {
            setUrl("https://maven.aliyun.com/repository/gradle-plugin")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            setUrl("https://maven.aliyun.com/repository/public")
        }
        maven {
            setUrl("https://maven.aliyun.com/repository/google")
        }
        mavenCentral()
    }
}

include(":server")
include(":malenia-api")
include(":frontend")
include(":doc")