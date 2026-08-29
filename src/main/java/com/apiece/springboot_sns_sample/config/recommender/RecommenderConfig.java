package com.apiece.springboot_sns_sample.config.recommender;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class RecommenderConfig {

    /**
     * 추천 서비스 전용 RestClient.
     *
     * <p>스프링이 자동 구성한 {@link RestClient.Builder} 를 그대로 받아서 쓴다. 이 빌더에는 관측
     * 기능이 붙어 있어서 호출마다 client span 이 생기고, 나가는 요청 헤더에 {@code traceparent} 가
     * 자동으로 실린다. 추천 서비스는 그 헤더를 이어받아 같은 트레이스에 자기 Span 을 붙인다.
     * 빌더를 새로 만들면 이 연결이 끊긴다.
     *
     * <p>읽기 타임아웃은 반드시 지정한다. 기본값은 무제한이라, 추천 서비스가 응답하지 않으면
     * 타임라인 요청이 그만큼 같이 묶여 있게 된다.
     */
    @Bean
    public RestClient recommenderRestClient(RestClient.Builder builder, RecommenderProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
