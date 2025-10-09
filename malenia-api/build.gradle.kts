plugins {
    id("java-library")
    id("org.gradle.groovy")
}

group = "cn.iinti.malenia2"
version = "1.0"

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    api("org.apache.commons:commons-lang3:3.12.0")
    api("commons-io:commons-io:2.10.0")
    api("com.alibaba:fastjson:1.2.79")
    api("org.jsoup:jsoup:1.15.3")
    api("com.google.guava:guava:31.1-jre")
    api("org.apache.groovy:groovy:4.0.23")
    api("org.apache.groovy:groovy-json:4.0.23")
}

tasks.test {
    useJUnitPlatform()
}