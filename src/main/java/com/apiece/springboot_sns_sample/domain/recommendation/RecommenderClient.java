package com.apiece.springboot_sns_sample.domain.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 추천 서비스(sns-recommender) 호출 클라이언트.
 *
 * <p>후보 게시글 목록을 보내면 사용자에게 맞춰 재정렬된 순서를 돌려준다. 추천 서비스는 Go 로
 * 작성된 별도 프로세스이므로, 이 호출은 서비스 경계를 넘는다. 나가는 요청에 실리는
 * {@code traceparent} 헤더 덕분에 두 서비스의 Span 이 하나의 트레이스로 이어진다.
 *
 * <p>실패하면 {@link RecommenderException} 을 던진다. 추천 없이 응답할지 요청 자체를 실패시킬지는
 * 호출하는 쪽이 정할 문제라서, 이 클래스는 폴백을 하지 않는다.
 */
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
