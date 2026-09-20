package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.domain.recommendation.RecommenderClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilityDemoControllerTest {

    @Test
    void recommendationTimeoutReturnsBadGateway() throws Exception {
        RecommenderClient client = mock(RecommenderClient.class);
        when(client.rank(3L, List.of(101L, 102L, 103L, 104L, 105L)))
                .thenThrow(new ResourceAccessException("request timed out"));

        MockMvcBuilders.standaloneSetup(new ObservabilityDemoController(client)).build()
                .perform(get("/api/v1/demo/trace").param("userId", "3"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
