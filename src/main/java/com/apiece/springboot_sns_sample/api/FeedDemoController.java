package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.config.recommend.RecommendProperties;
import com.apiece.springboot_sns_sample.controller.dto.FeedResponse;
import com.apiece.springboot_sns_sample.domain.recommend.RecommendClient;
import org.springframework.web.client.RestClientException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class FeedDemoController {

    private static final List<Long> FEED_CANDIDATES = List.of(101L, 102L, 103L, 104L, 105L);
    private static final String UNKNOWN_SEGMENT = "unknown";

    private final Tracer tracer;
    private final RecommendClient recommendClient;
    private final long timeoutMs;

    public FeedDemoController(OpenTelemetry openTelemetry,
                              RecommendClient recommendClient,
                              RecommendProperties recommendProperties) {
        this.tracer = openTelemetry.getTracer("sns-app.feed-demo");
        this.recommendClient = recommendClient;
        this.timeoutMs = recommendProperties.timeout().toMillis();
    }

    @GetMapping("/api/v1/demo/feed")
    public ResponseEntity<FeedResponse> feed(@RequestParam(defaultValue = "1") long userId) {
        Span span = tracer.spanBuilder("recommend-fetch")
                .setAttribute("user.id", userId)
                .setAttribute("timeout.ms", timeoutMs)
                .startSpan();
        String segment = UNKNOWN_SEGMENT;
        try (Scope ignored = span.makeCurrent()) {
            // 추천 호출이 실패해도 로그와 Span에 사용자 그룹을 남기기 위해 먼저 조회합니다.
            segment = recommendClient.segmentOf(userId);
            List<Long> rankedPostIds = recommendClient.rank(userId, FEED_CANDIDATES);

            log.info("피드 응답 완료 userId={} segment={} items={}", userId, segment, rankedPostIds.size());
            return ResponseEntity.ok(new FeedResponse(segment, rankedPostIds));
        } catch (RestClientException e) {
            span.setStatus(StatusCode.ERROR, "recommend timeout");
            span.recordException(e);
            log.error("추천 서비스 호출 실패 userId={} segment={} timeout={}ms",
                    userId, segment, timeoutMs, e);
            return ResponseEntity.status(503).body(new FeedResponse(segment, null));
        } finally {
            span.setAttribute("user.segment", segment);
            span.end();
        }
    }
}
