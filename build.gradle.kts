plugins {
    id("java")
}

group = "io.gymcrm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.spring.orm)
    implementation(libs.hibernate.core)
    implementation(libs.hikari)
    implementation(libs.logback.classic)
    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.spring.test)
    testRuntimeOnly(libs.h2)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

tasks.test {
    useJUnitPlatform()
}
