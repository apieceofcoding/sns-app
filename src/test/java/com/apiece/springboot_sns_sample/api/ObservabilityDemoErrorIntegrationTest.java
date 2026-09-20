package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.config.auth.AuthSuccessHandler;
import com.apiece.springboot_sns_sample.config.auth.SecurityConfig;
import com.apiece.springboot_sns_sample.domain.recommendation.RecommendService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ObservabilityDemoErrorIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ObservabilityDemoErrorIntegrationTest {
    @LocalServerPort
    int port;

    @Test
    void anonymousRecommendationFailureReturnsDefault500() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(
                            URI.create("http://localhost:" + port + "/api/v1/demo/trace?userId=3"))
                    .header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
            var body = JsonMapper.builder().build().readTree(response.body());
            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(body.path("status").asInt()).isEqualTo(500);
            assertThat(body.path("error").asText()).isEqualTo("Internal Server Error");
            assertThat(body.path("path").asText()).isEqualTo("/api/v1/demo/trace");
            assertThat(body.has("timestamp")).isTrue();
            assertThat(body.has("message")).isFalse();
            assertThat(body.has("trace")).isFalse();

            var protectedResponse = client.send(HttpRequest.newBuilder(
                            URI.create("http://localhost:" + port + "/private"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(protectedResponse.statusCode()).isEqualTo(401);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ObservabilityDemoController.class)
    @ImportAutoConfiguration({DispatcherServletAutoConfiguration.class, TomcatServletWebServerAutoConfiguration.class, WebMvcAutoConfiguration.class,
            ErrorMvcAutoConfiguration.class, JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
            SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
    static class TestConfig {
        @Bean
        RecommendService recommendService() {
            var service = mock(RecommendService.class);
            when(service.recommend(3L)).thenThrow(new ResourceAccessException("request timed out"));
            return service;
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) {
            return new SecurityConfig().securityFilterChain(http,
                    mock(SessionRegistry.class), mock(AuthSuccessHandler.class));
        }
    }
}
