package com.apiece.springboot_sns_sample.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo")
public class ObservabilityDemoController {

    @GetMapping("/trace")
    public ResponseEntity<Map<String, String>> trace(@RequestParam(defaultValue = "hello") String message) {
        log.info("[STEP 1] Request received: message={}", message);

        log.info("[STEP 2] Processing business logic");
        simulateWork(50);

        log.info("[STEP 3] Calling external service");
        simulateWork(30);

        log.info("[STEP 4] Request completed successfully");

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", message
        ));
    }

    @GetMapping("/error")
    public ResponseEntity<Map<String, String>> error() {
        log.info("[STEP 1] Request received for error simulation");
        log.warn("[STEP 2] Something looks wrong");

        try {
            simulateWork(20);
            throw new RuntimeException("Simulated error for observability demo");
        } catch (RuntimeException e) {
            log.error("[STEP 3] Error occurred: {}", e.getMessage(), e);
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
