package com.apiece.springboot_sns_sample.config.recommender;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "recommender")
public record RecommenderProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration timeout
) {
}
