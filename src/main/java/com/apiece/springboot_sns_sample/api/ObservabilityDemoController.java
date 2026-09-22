package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.demo.ErrorResponse;
import com.apiece.springboot_sns_sample.api.demo.TraceResponse;
import com.apiece.springboot_sns_sample.domain.recommend.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/demo")
public class ObservabilityDemoController {

    private final RecommendService recommendService;

    @GetMapping("/trace")
    public ResponseEntity<TraceResponse> trace(
            @RequestParam(defaultValue = "hello") String message,
            @RequestParam(defaultValue = "1") Long userId
    ) {
        log.info("[STEP 1] 요청 수신 message={} userId={}", message, userId);

        List<Long> rankedPostIds = recommendService.recommend(userId);

        log.info("[STEP 3] 요청 처리 완료");

        return ResponseEntity.ok(new TraceResponse(message, rankedPostIds));
    }

    @GetMapping("/ok")
    public ResponseEntity<String> ok() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/slow")
    public ResponseEntity<String> slow() throws InterruptedException {
        Thread.sleep(2000);
        return ResponseEntity.ok("slow");
    }

    @GetMapping("/error")
    public ResponseEntity<ErrorResponse> error() {
        log.info("[STEP 1] 오류 재현 요청 수신");
        log.warn("[STEP 2] 처리 중 이상 징후 발견");

        try {
            throw new RuntimeException("Simulated error for observability demo");
        } catch (RuntimeException e) {
            log.error("[STEP 3] 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ErrorResponse("error", e.getMessage()));
        }
    }
}
