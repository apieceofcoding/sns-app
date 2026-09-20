package com.apiece.springboot_sns_sample.domain.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendClient {

    private final RestClient recommendRestClient;

    public List<Long> rank(Long userId, List<Long> postIds) {
        RankResponse response = recommendRestClient.post()
                .uri("/v1/rank")
                .body(new RankRequest(userId, postIds))
                .retrieve()
                .body(RankResponse.class);

        if (response == null || response.rankedPostIds() == null) {
            throw new RestClientException("추천 서비스가 빈 응답을 반환했습니다 userId=" + userId);
        }

        log.info("추천 정렬 완료 userId={} segment={} candidates={} tookMs={}",
                userId, response.segment(), postIds.size(), response.tookMs());

        return response.rankedPostIds();
    }
}
