import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    alias(libs.plugins.spring.boot)
}

group = "io.gymcrm"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot's BOM picks compatible versions for everything below that has no version.
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    annotationProcessor(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.swagger.annotations)
    runtimeOnly(libs.postgresql)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.h2)
}

springBoot {
    buildInfo()
}

tasks.test {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}
