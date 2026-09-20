package com.apiece.springboot_sns_sample.config.recommend;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RecommendConfig {

    @Bean
    public RestClient recommendRestClient(RestClient.Builder builder,
                                           ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
                                           HttpClientSettings settings,
                                           RecommendProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactoryBuilder.build(
                        settings.withTimeouts(properties.connectTimeout(), properties.timeout())))
                .build();
    }
}
