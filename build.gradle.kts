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
    runtimeOnly(libs.micrometer.registry.prometheus)
    implementation(libs.swagger.annotations)
    runtimeOnly(libs.postgresql)
    // The in-memory database of the "standalone" profile (and of the tests).
    runtimeOnly(libs.h2)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

springBoot {
    buildInfo()
}

// One executable jar with a fixed name (used by the Dockerfile) and no extra "-plain" jar.
tasks.bootJar {
    archiveFileName = "gym-crm.jar"
}

tasks.jar {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}
