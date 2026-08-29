package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.domain.recommendation.RecommenderClient;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/demo")
public class ObservabilityDemoController {

    /** 타임라인 한 페이지를 흉내 낸 고정 후보 목록. 로그인 없이 호출할 수 있도록 값을 고정했다. */
    private static final List<Long> DEMO_CANDIDATES = List.of(101L, 102L, 103L, 104L, 105L);

    private final RecommenderClient recommenderClient;

    /**
     * 트레이스 실습용 엔드포인트.
     *
     * <p>타임라인과 같은 방식으로 추천 서비스를 호출한다. 이 호출이 서비스 경계를 넘으므로
     * 하나의 트레이스 안에 sns-app 과 sns-recommender 의 Span 이 함께 나타난다.
     */
    @GetMapping("/trace")
    public ResponseEntity<Map<String, Object>> trace(
            @RequestParam(defaultValue = "hello") String message,
            @RequestParam(defaultValue = "1") Long userId
    ) {
        log.info("[STEP 1] 요청 수신 message={} userId={}", message, userId);

        log.info("[STEP 2] 비즈니스 로직 처리");
        simulateWork(50);

        log.info("[STEP 3] 추천 서비스 호출");
        List<Long> rankedPostIds = recommenderClient.rank(userId, DEMO_CANDIDATES);

        log.info("[STEP 4] 요청 처리 완료");

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", message,
                "rankedPostIds", rankedPostIds
        ));
    }

    @GetMapping("/error")
    public ResponseEntity<Map<String, String>> error() {
        log.info("[STEP 1] 오류 재현 요청 수신");
        log.warn("[STEP 2] 처리 중 이상 징후 발견");

        try {
            simulateWork(20);
            throw new RuntimeException("Simulated error for observability demo");
        } catch (RuntimeException e) {
            log.error("[STEP 3] 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
