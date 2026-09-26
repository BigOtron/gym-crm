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
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype)
    implementation(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

tasks.test {
    useJUnitPlatform()
}