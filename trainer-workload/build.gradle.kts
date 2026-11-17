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
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation(platform(libs.spring.cloud.dependencies))
    annotationProcessor(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.cloud.starter.eureka.client)
    implementation(libs.swagger.annotations)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.bootJar {
    archiveFileName = "trainer-workload.jar"
}

tasks.jar {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
    finalizedBy(tasks.jacocoTestReport)
}

// Line coverage report in build/reports/jacoco; "check" fails below 80%.
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
