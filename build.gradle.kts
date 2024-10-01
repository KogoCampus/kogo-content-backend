import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    val kotlinVersion = "2.0.0"
    val springVersion = "3.3.1"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version springVersion
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
    // id("org.jlleitschuh.gradle.ktlint").version("12.1.0")
    id("it.nicolasfarabegoli.conventional-commits") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.kogo"
version = "0.0.1-SNAPSHOT"
description = "kogo-content-backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("jakarta.validation:jakarta.validation-api:3.1.0")
    // Spring Cloud
    implementation("io.awspring.cloud:spring-cloud-starter-aws-secrets-manager-config:2.4.4")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:3.0.0")
    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security.oauth.boot:spring-security-oauth2-autoconfigure:2.6.8")
    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    // Logging
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.apache.logging.log4j:log4j-spring-boot:$log4jVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")
    // Testing
    implementation("com.google.code.gson:gson")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
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

/*
* Swagger docs generation configuration
*/
openApi {
    outputFileName.set("swagger.json")
    outputDir.set(file("src/main/swagger-ui"))
    apiDocsUrl.set("http://localhost:8080/api-docs")
}

conventionalCommits {
    val admittedTypes = listOf(
        "feat",
        "test",
        "fix",
        "chore",
        "ci",
        "docs",
        "revert")
    warningIfNoGitRoot = true
    types = admittedTypes
    scopes = emptyList()
    successMessage = "Commit message meets Conventional Commit standards..."
    failureMessage = """
        The commit message does not meet the Conventional Commit standard.
        Admitted Types: ${admittedTypes.toString()}.
        e.g.
        git commit -m 'chore: upgrade convetional commit plugin version to 3.1.3'
    """.trimIndent()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<BootBuildImage>("bootBuildImage") {
    // For multi arch (Apple Silicon) support
    builder.set("paketobuildpacks/builder-jammy-buildpackless-tiny")
    buildpacks.set(listOf("paketobuildpacks/java"))
}