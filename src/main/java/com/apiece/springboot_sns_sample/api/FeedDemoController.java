package com.apiece.springboot_sns_sample.api;

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

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 9강 장애 분석 실습용 엔드포인트.
 *
 * <p>추천 피드를 돌려주는 평범한 API 처럼 보이지만, beta 세그먼트로 분류된 요청만
 * 느린 추천 서비스를 타고 타임아웃으로 실패한다. 세그먼트는 userId 로 결정되므로
 * 재현이 가능하다.
 *
 * <p>세 신호를 따로 보면 원인이 안 나오도록 만든 것이 핵심이다.
 * 메트릭은 "에러율이 올랐다"까지만, 트레이스는 "recommendation-fetch 구간이 느리다"까지만,
 * 로그는 "beta 세그먼트만 실패한다"까지만 알려준다. 셋을 합쳐야 결론이 선다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo")
public class FeedDemoController {

    /** 추천 서비스 호출 타임아웃. 실패의 직접 원인이다. */
    private static final long TIMEOUT_MS = 200;

    private final Tracer tracer;

    public FeedDemoController(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("sns-app.feed-demo");
    }

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> feed(@RequestParam(defaultValue = "1") long userId) {
        String segment = (userId % 3 == 0) ? "beta" : "ga";

        Span span = tracer.spanBuilder("recommendation-fetch").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute("user.id", userId);
            span.setAttribute("user.segment", segment);
            span.setAttribute("timeout.ms", TIMEOUT_MS);

            long took = callRecommendationService(segment);
            span.setAttribute("upstream.duration_ms", took);

            if (took > TIMEOUT_MS) {
                span.setStatus(StatusCode.ERROR, "recommendation timeout");
                log.error("추천 서비스 타임아웃 userId={} segment={} took={}ms timeout={}ms",
                        userId, segment, took, TIMEOUT_MS);
                return ResponseEntity.status(503).body(Map.of(
                        "status", "error",
                        "reason", "recommendation_timeout",
                        "segment", segment));
            }

            log.info("피드 응답 완료 userId={} segment={} took={}ms", userId, segment, took);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "segment", segment,
                    "items", 20));
        } finally {
            span.end();
        }
    }

    /**
     * 추천 서비스 호출을 흉내 낸다. beta 세그먼트는 최적화가 안 된 경로를 타서
     * 타임아웃보다 오래 걸린다.
     */
    private long callRecommendationService(String segment) {
        long base = "beta".equals(segment) ? 260 : 40;
        long jitter = ThreadLocalRandom.current().nextLong(60);
        long took = base + jitter;
        sleep(took);
        return took;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
