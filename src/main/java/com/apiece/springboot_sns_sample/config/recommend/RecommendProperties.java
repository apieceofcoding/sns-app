package com.apiece.springboot_sns_sample.config.recommend;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "recommend")
public record RecommendProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration timeout
) {
}
