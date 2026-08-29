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

/**
 * 9강 장애 분석 실습용 엔드포인트.
 *
 * <p>추천 피드를 돌려주는 평범한 API 처럼 보이지만, beta 세그먼트로 분류된 요청만 느린 추천
 * 경로를 타서 타임아웃으로 실패한다. 세그먼트는 userId 로 결정되므로 재현이 가능하다.
 *
 * <p>타임라인({@code TimelineService})은 추천이 실패해도 시간순으로 폴백하지만, 이 엔드포인트는
 * 일부러 폴백을 두지 않았다. 폴백이 없는 호출이 어떻게 장애가 되는지가 이번 실습의 소재다.
 *
 * <p>세 신호를 따로 보면 원인이 안 나오도록 만든 것이 핵심이다. 메트릭은 "에러율이 올랐다"까지만,
 * 트레이스는 "recommendation-fetch 구간이 타임아웃으로 끊겼다"까지만, 로그는 "beta 세그먼트만
 * 실패한다"까지만 알려준다. 셋을 합쳐야 결론이 선다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo")
public class FeedDemoController {

    /** 피드 한 페이지를 흉내 낸 고정 후보 목록. */
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

    /**
     * 세그먼트를 호출하는 쪽에서도 계산한다. 추천 서비스가 응답하지 않으면 세그먼트를 응답에서
     * 알 수 없는데, 실패를 어느 집단에 귀속시킬지는 그때가 가장 중요하기 때문이다.
     */
    private String segmentOf(long userId) {
        return (userId % 3 == 0) ? "beta" : "ga";
    }
}
