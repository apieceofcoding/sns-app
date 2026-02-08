plugins {
    java
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "9.1.0"
    id("com.diffplug.spotless") version "8.1.0"
}

group = "com.apiece"
version = "0.0.1-SNAPSHOT"
description = "SNS sample project for Spring Boot"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.bouncycastle:bcprov-jdk18on:1.82")
    implementation("software.amazon.awssdk:s3:2.29.26")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    java {
        target("src/**/*.java")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }

    json {
        target("src/**/*.json", ".claude/**/*.json")
        gson()
            .indentWithSpaces(2)
            .sortByKeys()
        trimTrailingWhitespace()
        endWithNewline()
    }

    yaml {
        target("src/**/*.yaml", "src/**/*.yml")
        jackson()
            .yamlFeature("WRITE_DOC_START_MARKER", false)
            .yamlFeature("MINIMIZE_QUOTES", true)
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("markdown") {
        target("*.md", ".claude/**/*.md")
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("misc") {
        target("src/**/*.properties", "src/**/*.xml", "src/**/*.sql", "src/**/*.sh")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
