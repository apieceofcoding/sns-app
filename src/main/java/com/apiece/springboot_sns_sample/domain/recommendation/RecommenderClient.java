package com.apiece.springboot_sns_sample.domain.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommenderClient {

    private final RestClient recommenderRestClient;

    public List<Long> rank(Long userId, List<Long> postIds) {
        RankResponse response;
        try {
            response = recommenderRestClient.post()
                    .uri("/v1/rank")
                    .body(new RankRequest(userId, postIds))
                    .retrieve()
                    .body(RankResponse.class);
        } catch (Exception e) {
            throw new RecommenderException("추천 서비스 호출에 실패했습니다 userId=" + userId, e);
        }

        if (response == null || response.rankedPostIds() == null) {
            throw new RecommenderException("추천 서비스가 빈 응답을 반환했습니다 userId=" + userId);
        }

        log.info("추천 정렬 완료 userId={} segment={} candidates={} tookMs={}",
                userId, response.segment(), postIds.size(), response.tookMs());

        return response.rankedPostIds();
    }
}
