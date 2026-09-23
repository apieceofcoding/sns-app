package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.config.recommend.RecommendProperties;
import com.apiece.springboot_sns_sample.controller.dto.FeedResponse;
import com.apiece.springboot_sns_sample.domain.recommend.RecommendClient;
import org.springframework.web.client.RestClientException;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FeedDemoController {

    private static final List<Long> FEED_CANDIDATES = List.of(101L, 102L, 103L, 104L, 105L);

    private final RecommendClient recommendClient;
    private final RecommendProperties recommendProperties;

    @NewSpan("recommend-fetch")
    @GetMapping("/api/v1/demo/feed")
    public ResponseEntity<FeedResponse> feed(@RequestParam(defaultValue = "1") long userId) {
        long timeoutMs = recommendProperties.timeout().toMillis();
        Span span = Span.current()
                .setAttribute("user.id", userId)
                .setAttribute("timeout.ms", timeoutMs);
        try {
            List<Long> rankedPostIds = recommendClient.rank(userId, FEED_CANDIDATES);

            log.info("피드 응답 완료 userId={} items={}", userId, rankedPostIds.size());
            return ResponseEntity.ok(new FeedResponse(rankedPostIds));
        } catch (RestClientException e) {
            span.setStatus(StatusCode.ERROR, "recommend request failed");
            span.recordException(e);
            log.error("추천 서비스 호출 실패 userId={} timeout={}ms",
                    userId, timeoutMs, e);
            return ResponseEntity.status(503).body(new FeedResponse(null));
        }
    }
}
