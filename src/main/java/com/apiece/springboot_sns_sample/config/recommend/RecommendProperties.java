package com.apiece.springboot_sns_sample.config.recommend;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "recommender")
public record RecommendProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration timeout
) {
}
