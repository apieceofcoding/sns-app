package com.apiece.springboot_sns_sample.domain.recommend;

import com.apiece.springboot_sns_sample.config.recommend.RecommendConfig;
import com.apiece.springboot_sns_sample.config.recommend.RecommendProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommendClientTest {

    private static final Duration TIMEOUT = Duration.ofMillis(200);

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("추천 서비스가 돌려준 순서를 그대로 반환한다")
    void returnsRankedOrder() throws IOException {
        startServer(exchange -> respond(exchange, 200, """
                {"userId":7,"segment":"ga","rankedPostIds":[103,101,102],"tookMs":41}
                """));

        List<Long> ranked = client().rank(7L, List.of(101L, 102L, 103L));

        assertThat(ranked).containsExactly(103L, 101L, 102L);
    }

    @Test
    @DisplayName("후보 목록과 userId 를 본문에 담아 보낸다")
    void sendsCandidatesInBody() throws IOException {
        AtomicReference<String> received = new AtomicReference<>();
        startServer(exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"userId":7,"segment":"ga","rankedPostIds":[101],"tookMs":40}
                    """);
        });

        client().rank(7L, List.of(101L));

        assertThat(received.get()).contains("\"userId\":7").contains("\"postIds\":[101]");
    }

    @Test
    @DisplayName("타임아웃을 넘기면 RestClientException 을 던진다")
    void throwsWhenSlowerThanTimeout() throws IOException {
        startServer(exchange -> {
            sleep(TIMEOUT.toMillis() * 3);
            respond(exchange, 200, """
                    {"userId":7,"segment":"beta","rankedPostIds":[101],"tookMs":600}
                    """);
        });

        assertThatThrownBy(() -> client().rank(7L, List.of(101L)))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("추천 서비스가 5xx 를 반환하면 RestClientException 을 던진다")
    void throwsOnServerError() throws IOException {
        startServer(exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"));

        assertThatThrownBy(() -> client().rank(7L, List.of(101L)))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("응답에 순서가 없으면 RestClientException 을 던진다")
    void throwsOnMissingRankedIds() throws IOException {
        startServer(exchange -> respond(exchange, 200, "{\"userId\":7,\"segment\":\"ga\"}"));

        assertThatThrownBy(() -> client().rank(7L, List.of(101L)))
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("빈 응답");
    }

    @Test
    @DisplayName("Boot 자동 구성과 RestClient 커스터마이저를 유지한다")
    void preservesBootAutoConfiguration() throws IOException {
        AtomicReference<String> header = new AtomicReference<>();
        startServer(exchange -> {
            header.set(exchange.getRequestHeaders().getFirst("X-Lesson"));
            respond(exchange, 200, """
                    {"userId":7,"segment":"ga","rankedPostIds":[101],"tookMs":40}
                    """);
        });

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class,
                        ImperativeHttpClientAutoConfiguration.class, RestClientAutoConfiguration.class))
                .withUserConfiguration(RecommendConfig.class)
                .withBean(RecommendProperties.class, () -> new RecommendProperties(
                        "http://localhost:" + server.getAddress().getPort(), TIMEOUT, TIMEOUT))
                .withBean(RestClientCustomizer.class, () -> builder -> builder.defaultHeader("X-Lesson", "traces"))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RestClient.class);
                    assertThat(new RecommendClient(context.getBean(RestClient.class)).rank(7L, List.of(101L)))
                            .containsExactly(101L);
                    assertThat(header.get()).isEqualTo("traces");
                });
    }

    private RecommendClient client() {
        RecommendProperties properties = new RecommendProperties(
                "http://localhost:" + server.getAddress().getPort(),
                TIMEOUT,
                TIMEOUT
        );
        RestClient restClient = new RecommendConfig()
                .recommendRestClient(RestClient.builder(), ClientHttpRequestFactoryBuilder.detect(),
                        HttpClientSettings.defaults(), properties);
        return new RecommendClient(restClient);
    }

    private void startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/rank", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
