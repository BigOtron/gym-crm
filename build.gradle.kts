plugins {
    id("java")
}

group = "io.gymcrm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.webmvc)
    implementation(libs.tomcat.embed.core)
    implementation(libs.jackson.databind)
    implementation(libs.hibernate.validator)
    runtimeOnly(libs.expressly)
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
    testImplementation(libs.json.path)
    testImplementation(libs.hamcrest)
    testImplementation(libs.jsonassert)
    testRuntimeOnly(libs.h2)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

// Keep method parameter names in the compiled classes, so Spring MVC can match
// @PathVariable String username to {username}. Spring Boot's Gradle plugin does this automatically.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}
