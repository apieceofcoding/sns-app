package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.domain.recommend.RecommendClient;
import com.apiece.springboot_sns_sample.domain.recommend.RecommendService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilityDemoControllerTest {

    @Test
    void okReturnsWithoutCallingRecommend() throws Exception {
        RecommendClient client = mock(RecommendClient.class);
        MockMvcBuilders.standaloneSetup(new ObservabilityDemoController(new RecommendService(client), new com.apiece.springboot_sns_sample.config.recommend.RecommendProperties("http://localhost", java.time.Duration.ofMillis(200), java.time.Duration.ofMillis(200)), io.opentelemetry.api.OpenTelemetry.noop())).build()
                .perform(get("/api/v1/demo/ok"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
        org.mockito.Mockito.verifyNoInteractions(client);
    }

    @Test
    void slowTakesAtLeastTwoSecondsWithoutCallingRecommend() throws Exception {
        RecommendClient client = mock(RecommendClient.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ObservabilityDemoController(new RecommendService(client), new com.apiece.springboot_sns_sample.config.recommend.RecommendProperties("http://localhost", java.time.Duration.ofMillis(200), java.time.Duration.ofMillis(200)), io.opentelemetry.api.OpenTelemetry.noop())).build();
        long start = System.nanoTime();
        mvc.perform(get("/api/v1/demo/slow"))
                .andExpect(status().isOk())
                .andExpect(content().string("slow"));
        assertTrue(System.nanoTime() - start >= 2_000_000_000L);
        org.mockito.Mockito.verifyNoInteractions(client);
    }

    @Test
    void errorReturnsInternalServerError() throws Exception {
        RecommendClient client = mock(RecommendClient.class);

        MockMvcBuilders.standaloneSetup(new ObservabilityDemoController(new RecommendService(client), new com.apiece.springboot_sns_sample.config.recommend.RecommendProperties("http://localhost", java.time.Duration.ofMillis(200), java.time.Duration.ofMillis(200)), io.opentelemetry.api.OpenTelemetry.noop())).build()
                .perform(get("/api/v1/demo/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("error"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.message").value("Simulated error for observability demo"));
        org.mockito.Mockito.verifyNoInteractions(client);
    }

    @Test
    void traceReturnsRecommendedOrder() throws Exception {
        RecommendClient client = mock(RecommendClient.class);
        when(client.rank(1L, List.of(101L, 102L, 103L, 104L, 105L)))
                .thenReturn(List.of(105L, 104L, 103L, 102L, 101L));

        MockMvcBuilders.standaloneSetup(new ObservabilityDemoController(new RecommendService(client), new com.apiece.springboot_sns_sample.config.recommend.RecommendProperties("http://localhost", java.time.Duration.ofMillis(200), java.time.Duration.ofMillis(200)), io.opentelemetry.api.OpenTelemetry.noop())).build()
                .perform(get("/api/v1/demo/trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.message").value("hello"))
                .andExpect(jsonPath("$.rankedPostIds[0]").value(105));
    }

}
