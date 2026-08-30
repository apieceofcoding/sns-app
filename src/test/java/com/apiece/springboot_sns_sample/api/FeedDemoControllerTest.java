package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.config.recommender.RecommenderConfig;
import com.apiece.springboot_sns_sample.config.recommender.RecommenderProperties;
import com.apiece.springboot_sns_sample.domain.recommendation.RecommenderClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FeedDemoControllerTest {

    private static final Duration TIMEOUT = Duration.ofMillis(200);
    private static final AttributeKey<String> USER_SEGMENT = AttributeKey.stringKey("user.segment");
    private static final AttributeKey<Long> TIMEOUT_MS = AttributeKey.longKey("timeout.ms");

    private static final String FAST_RANK = "{\"userId\":7,\"segment\":\"ga\",\"rankedPostIds\":[101],\"tookMs\":40}";

    private final CapturingSpanProcessor spans = new CapturingSpanProcessor();

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("세그먼트는 앱이 계산하지 않고 추천 서비스가 판정한 값을 그대로 쓴다")
    void usesSegmentFromRecommender() throws IOException {
        startServer(
                exchange -> respond(exchange, 200, "{\"userId\":7,\"segment\":\"beta\"}"),
                exchange -> respond(exchange, 200, FAST_RANK));

        ResponseEntity<Map<String, Object>> response = controller().feed(7L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("segment", "beta");
        assertThat(endedSpan().getAttributes().get(USER_SEGMENT)).isEqualTo("beta");
    }

    @Test
    @DisplayName("추천 호출이 타임아웃으로 실패해도 조회해둔 세그먼트가 503 응답과 Span 에 남는다")
    void keepsSegmentWhenRankTimesOut() throws IOException {
        startServer(
                exchange -> respond(exchange, 200, "{\"userId\":3,\"segment\":\"beta\"}"),
                exchange -> {
                    sleep(TIMEOUT.toMillis() * 3);
                    respond(exchange, 200, FAST_RANK);
                });

        ResponseEntity<Map<String, Object>> response = controller().feed(3L);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsEntry("reason", "recommendation_timeout")
                .containsEntry("segment", "beta");

        SpanData span = endedSpan();
        assertThat(span.getName()).isEqualTo("recommendation-fetch");
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getAttributes().get(USER_SEGMENT)).isEqualTo("beta");
        assertThat(span.getAttributes().get(TIMEOUT_MS)).isEqualTo(TIMEOUT.toMillis());
    }

    @Test
    @DisplayName("세그먼트 조회 자체가 실패하면 unknown 으로 남고 추천 호출은 시도하지 않는다")
    void marksSegmentUnknownWhenLookupFails() throws IOException {
        AtomicInteger rankCalls = new AtomicInteger();
        startServer(
                exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"),
                exchange -> {
                    rankCalls.incrementAndGet();
                    respond(exchange, 200, FAST_RANK);
                });

        ResponseEntity<Map<String, Object>> response = controller().feed(3L);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).containsEntry("segment", "unknown");
        assertThat(rankCalls).hasValue(0);

        SpanData span = endedSpan();
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getAttributes().get(USER_SEGMENT)).isEqualTo("unknown");
    }

    @Test
    @DisplayName("user.segment 는 Span 하나에 한 번만 기록된다")
    void recordsSegmentExactlyOnce() throws IOException {
        startServer(
                exchange -> respond(exchange, 200, "{\"userId\":7,\"segment\":\"ga\"}"),
                exchange -> respond(exchange, 200, FAST_RANK));

        controller().feed(7L);

        assertThat(spans.ended).hasSize(1);
        assertThat(endedSpan().getTotalAttributeCount())
                .isEqualTo(endedSpan().getAttributes().size());
    }

    private FeedDemoController controller() {
        RecommenderProperties properties = new RecommenderProperties(
                "http://localhost:" + server.getAddress().getPort(),
                TIMEOUT,
                TIMEOUT
        );
        RestClient restClient = new RecommenderConfig()
                .recommenderRestClient(RestClient.builder(), properties);

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().addSpanProcessor(spans).build())
                .build();

        return new FeedDemoController(openTelemetry, new RecommenderClient(restClient), properties);
    }

    private SpanData endedSpan() {
        assertThat(spans.ended).hasSize(1);
        return spans.ended.getFirst();
    }

    private void startServer(HttpHandler segmentHandler, HttpHandler rankHandler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/segment", closing(segmentHandler));
        server.createContext("/v1/rank", closing(rankHandler));
        server.start();
    }

    private static HttpHandler closing(HttpHandler handler) {
        return exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        };
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

    private static final class CapturingSpanProcessor implements SpanProcessor {

        private final List<SpanData> ended = new CopyOnWriteArrayList<>();

        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {
        }

        @Override
        public boolean isStartRequired() {
            return false;
        }

        @Override
        public void onEnd(ReadableSpan span) {
            ended.add(span.toSpanData());
        }

        @Override
        public boolean isEndRequired() {
            return true;
        }
    }
}
