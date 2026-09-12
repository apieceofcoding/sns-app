package com.apiece.springboot_sns_sample.domain.recommendation;

import com.apiece.springboot_sns_sample.config.recommender.RecommenderConfig;
import com.apiece.springboot_sns_sample.config.recommender.RecommenderProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommenderClientTest {

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
    @DisplayName("타임아웃을 넘기면 RecommenderException 을 던진다")
    void throwsWhenSlowerThanTimeout() throws IOException {
        startServer(exchange -> {
            sleep(TIMEOUT.toMillis() * 3);
            respond(exchange, 200, """
                    {"userId":7,"segment":"beta","rankedPostIds":[101],"tookMs":600}
                    """);
        });

        assertThatThrownBy(() -> client().rank(7L, List.of(101L)))
                .isInstanceOf(RecommenderException.class)
                .hasMessageContaining("userId=7");
    }

    @Test
    @DisplayName("추천 서비스가 5xx 를 반환하면 RecommenderException 을 던진다")
    void throwsOnServerError() throws IOException {
        startServer(exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"));

        assertThatThrownBy(() -> client().rank(7L, List.of(101L)))
                .isInstanceOf(RecommenderException.class);
    }

    @Test
    @DisplayName("응답에 순서가 없으면 RecommenderException 을 던진다")
    void throwsOnMissingRankedIds() throws IOException {
        startServer(exchange -> respond(exchange, 200, "{\"userId\":7,\"segment\":\"ga\"}"));

        assertThatThrownBy(() -> client().rank(7L, List.of(101L)))
                .isInstanceOf(RecommenderException.class)
                .hasMessageContaining("빈 응답");
    }

    @Test
    @DisplayName("세그먼트는 직접 계산하지 않고 추천 서비스가 판정한 값을 쓴다")
    void asksRecommenderForSegment() throws IOException {
        AtomicReference<String> received = new AtomicReference<>();
        startServer("/v1/segment", exchange -> {
            received.set(exchange.getRequestURI().getQuery());
            respond(exchange, 200, "{\"userId\":7,\"segment\":\"beta\"}");
        });

        String segment = client().segmentOf(7L);

        assertThat(received.get()).isEqualTo("userId=7");
        assertThat(segment).isEqualTo("beta");
    }

    @Test
    @DisplayName("세그먼트 조회가 실패하면 RecommenderException 을 던진다")
    void throwsWhenSegmentLookupFails() throws IOException {
        startServer("/v1/segment", exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"));

        assertThatThrownBy(() -> client().segmentOf(7L))
                .isInstanceOf(RecommenderException.class)
                .hasMessageContaining("userId=7");
    }

    @Test
    @DisplayName("세그먼트 응답이 비어 있으면 RecommenderException 을 던진다")
    void throwsOnMissingSegment() throws IOException {
        startServer("/v1/segment", exchange -> respond(exchange, 200, "{\"userId\":7}"));

        assertThatThrownBy(() -> client().segmentOf(7L))
                .isInstanceOf(RecommenderException.class)
                .hasMessageContaining("빈 응답");
    }

    private RecommenderClient client() {
        RecommenderProperties properties = new RecommenderProperties(
                "http://localhost:" + server.getAddress().getPort(),
                TIMEOUT,
                TIMEOUT
        );
        RestClient restClient = new RecommenderConfig()
                .recommenderRestClient(RestClient.builder(), properties);
        return new RecommenderClient(restClient);
    }

    private void startServer(HttpHandler handler) throws IOException {
        startServer("/v1/rank", handler);
    }

    private void startServer(String path, HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(path, exchange -> {
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
