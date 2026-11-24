import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    jacoco
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
    implementation(platform(libs.spring.cloud.dependencies))
    annotationProcessor(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.actuator)
    runtimeOnly(libs.micrometer.registry.prometheus)
    implementation(libs.swagger.annotations)
    // Registers with Eureka and finds the Trainer Workload service there by name (load-balanced).
    implementation(libs.spring.cloud.starter.eureka.client)
    implementation(libs.spring.cloud.starter.circuitbreaker.resilience4j)
    implementation(libs.spring.boot.starter.activemq)
    // The load balancer's cache of service instances (otherwise it warns about its fallback cache).
    runtimeOnly(libs.caffeine)
    runtimeOnly(libs.postgresql)
    // The in-memory database of the "standalone" profile (and of the tests).
    runtimeOnly(libs.h2)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.activemq.broker)
    testRuntimeOnly(libs.junit.platform.launcher)
}

springBoot {
    buildInfo()
}

tasks.bootJar {
    archiveFileName = "gym-crm.jar"
}

tasks.jar {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
