package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.config.recommender.RecommenderProperties;
import com.apiece.springboot_sns_sample.domain.recommendation.RecommenderClient;
import com.apiece.springboot_sns_sample.domain.recommendation.RecommenderException;
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

    private final Tracer tracer;
    private final RecommenderClient recommenderClient;
    private final long timeoutMs;

    public FeedDemoController(OpenTelemetry openTelemetry,
                              RecommenderClient recommenderClient,
                              RecommenderProperties recommenderProperties) {
        this.tracer = openTelemetry.getTracer("sns-app.feed-demo");
        this.recommenderClient = recommenderClient;
        this.timeoutMs = recommenderProperties.timeout().toMillis();
    }

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> feed(@RequestParam(defaultValue = "1") long userId) {
        String segment = segmentOf(userId);

        Span span = tracer.spanBuilder("recommendation-fetch").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute("user.id", userId);
            span.setAttribute("user.segment", segment);
            span.setAttribute("timeout.ms", timeoutMs);

            List<Long> rankedPostIds = recommenderClient.rank(userId, FEED_CANDIDATES);

            log.info("피드 응답 완료 userId={} segment={} items={}", userId, segment, rankedPostIds.size());
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "segment", segment,
                    "postIds", rankedPostIds));
        } catch (RecommenderException e) {
            span.setStatus(StatusCode.ERROR, "recommendation timeout");
            span.recordException(e);
            log.error("추천 서비스 호출 실패 userId={} segment={} timeout={}ms",
                    userId, segment, timeoutMs, e);
            return ResponseEntity.status(503).body(Map.of(
                    "status", "error",
                    "reason", "recommendation_timeout",
                    "segment", segment));
        } finally {
            span.end();
        }
    }

    private String segmentOf(long userId) {
        return (userId % 3 == 0) ? "beta" : "ga";
    }
}
