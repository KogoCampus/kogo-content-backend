import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    val kotlinVersion = "2.0.0"
    id("org.springframework.boot") version "3.3.1"
    // id("org.jlleitschuh.gradle.ktlint").version("12.1.0")
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
}

group = "com.kogo"
version = "0.0.1-SNAPSHOT"
description = "kogo-content-backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    val log4jVersion = "2.17.1"
    implementation ("com.meilisearch.sdk:meilisearch-java:0.14.0")
    implementation ("org.json:json:20240303")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.security.oauth.boot:spring-security-oauth2-autoconfigure:2.6.8")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("com.google.code.gson:gson")
    implementation("org.apache.logging.log4j:log4j-spring-boot:$log4jVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")
    implementation("jakarta.validation:jakarta.validation-api:3.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo:4.13.0")
}

configurations {
    all {
        exclude("org.springframework.boot", "spring-boot-starter-logging")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<BootBuildImage>("bootBuildImage") {
    // For multi arch (Apple Silicon) support
    builder.set("paketobuildpacks/builder-jammy-buildpackless-tiny")
    buildpacks.set(listOf("paketobuildpacks/java"))
}
