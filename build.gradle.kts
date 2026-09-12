plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "9.5.0"
    id("com.diffplug.spotless") version "8.9.0"
    id("com.google.cloud.tools.jib") version "3.5.4"
}

jib {
    from {
        // 실행에 필요한 것은 JRE 뿐이라 jre 베이스를 쓴다
        image = "eclipse-temurin:25-jre-alpine"
        platforms {
            platform {
                // Apple Silicon 과 Intel 을 모두 지원하도록 빌드 머신 아키텍처를 따른다
                architecture = if (System.getProperty("os.arch") == "aarch64") "arm64" else "amd64"
                os = "linux"
            }
        }
    }
    to {
        image = "springboot-sns:latest"
    }
    container {
        ports = listOf("8080")
        // non-root 실행
        user = "1000:1000"
    }
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
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.24.0-alpha")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.bouncycastle:bcprov-jdk18on:1.85.2")
    implementation("software.amazon.awssdk:s3:2.53.0")

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
