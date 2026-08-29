package com.apiece.springboot_sns_sample.config.recommender;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 추천 서비스 연동 설정.
 *
 * <p>{@code timeout} 은 추천 응답을 얼마나 기다릴지 정한다. 이 값을 넘기면 요청이 실패하므로
 * 추천 서비스의 응답 시간보다 짧게 잡으면 장애가 된다.
 */
@ConfigurationProperties(prefix = "recommender")
public record RecommenderProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration timeout
) {
}
