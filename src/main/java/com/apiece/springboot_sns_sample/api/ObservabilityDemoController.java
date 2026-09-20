package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.domain.recommendation.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
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

    private final RecommendService recommendService;

    @GetMapping("/trace")
    public ResponseEntity<Map<String, Object>> trace(
            @RequestParam(defaultValue = "hello") String message,
            @RequestParam(defaultValue = "1") Long userId
    ) {
        log.info("[STEP 1] 요청 수신 message={} userId={}", message, userId);

        List<Long> rankedPostIds = recommendService.recommend(userId);

        log.info("[STEP 3] 요청 처리 완료");

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", message,
                "rankedPostIds", rankedPostIds
        ));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> recommendationFailed(RestClientException exception) {
        log.error("추천 서비스 호출 실패", exception);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("status", "error", "message", "추천 서비스 호출에 실패했습니다"));
    }

    @GetMapping("/error")
    public ResponseEntity<Map<String, String>> error() {
        log.info("[STEP 1] 오류 재현 요청 수신");
        log.warn("[STEP 2] 처리 중 이상 징후 발견");

        try {
            throw new RuntimeException("Simulated error for observability demo");
        } catch (RuntimeException e) {
            log.error("[STEP 3] 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}
