package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.config.recommend.RecommendProperties;
import com.apiece.springboot_sns_sample.domain.recommendation.RecommendClient;
import org.springframework.web.client.RestClientException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo")
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

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> feed(@RequestParam(defaultValue = "1") long userId) {
        Span span = tracer.spanBuilder("recommendation-fetch").startSpan();
        String segment = UNKNOWN_SEGMENT;
        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute("user.id", userId);
            span.setAttribute("timeout.ms", timeoutMs);

            segment = recommendClient.segmentOf(userId);
            List<Long> rankedPostIds = recommendClient.rank(userId, FEED_CANDIDATES);

            log.info("피드 응답 완료 userId={} segment={} items={}", userId, segment, rankedPostIds.size());
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "segment", segment,
                    "postIds", rankedPostIds));
        } catch (RestClientException e) {
            span.setStatus(StatusCode.ERROR, "recommendation timeout");
            span.recordException(e);
            log.error("추천 서비스 호출 실패 userId={} segment={} timeout={}ms",
                    userId, segment, timeoutMs, e);
            return ResponseEntity.status(503).body(Map.of(
                    "status", "error",
                    "reason", "recommendation_timeout",
                    "segment", segment));
        } finally {
            span.setAttribute("user.segment", segment);
            span.end();
        }
    }
}
