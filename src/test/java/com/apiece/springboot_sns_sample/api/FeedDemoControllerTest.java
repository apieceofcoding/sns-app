package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.config.recommend.RecommendConfig;
import com.apiece.springboot_sns_sample.controller.dto.FeedResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.json.JsonCompareMode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.apiece.springboot_sns_sample.config.recommend.RecommendProperties;
import com.apiece.springboot_sns_sample.domain.recommend.RecommendClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.PropertiesPropertySource;
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
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
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
    private AnnotationConfigApplicationContext context;
    private OpenTelemetrySdk openTelemetry;

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
        if (openTelemetry != null) {
            openTelemetry.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("세그먼트는 앱이 계산하지 않고 추천 서비스가 판정한 값을 그대로 쓴다")
    void usesSegmentFromRecommend() throws IOException {
        startServer(
                exchange -> respond(exchange, 200, "{\"userId\":7,\"segment\":\"beta\"}"),
                exchange -> respond(exchange, 200, FAST_RANK));

        ResponseEntity<FeedResponse> response = controller().feed(7L);

        assertThat(Span.current().getSpanContext().isValid()).isFalse();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().segment()).isEqualTo("beta");
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

        ResponseEntity<FeedResponse> response = controller().feed(3L);

        assertThat(Span.current().getSpanContext().isValid()).isFalse();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().segment()).isEqualTo("beta");

        SpanData span = endedSpan();
        assertThat(span.getName()).isEqualTo("recommend-fetch");
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getEvents()).anySatisfy(event -> assertThat(event.getName()).isEqualTo("exception"));
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

        ResponseEntity<FeedResponse> response = controller().feed(3L);

        assertThat(Span.current().getSpanContext().isValid()).isFalse();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().segment()).isEqualTo("unknown");
        assertThat(rankCalls).hasValue(0);

        SpanData span = endedSpan();
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getEvents()).anySatisfy(event -> assertThat(event.getName()).isEqualTo("exception"));
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

    @Test
    @DisplayName("피드 성공 응답은 segment와 postIds만 포함한다")
    void returnsOnlyFeedFieldsOnSuccess() throws Exception {
        startServer(
                exchange -> respond(exchange, 200, "{\"userId\":1,\"segment\":\"ga\"}"),
                exchange -> respond(exchange, 200, FAST_RANK));

        MockMvcBuilders.standaloneSetup(controller()).build()
                .perform(get("/api/v1/demo/feed"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"segment":"ga","postIds":[101]}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("피드 실패 응답은 segment와 null postIds를 포함하고 HTTP 503을 반환한다")
    void returnsNullPostIdsOnFailure() throws Exception {
        startServer(
                exchange -> respond(exchange, 200, "{\"userId\":3,\"segment\":\"beta\"}"),
                exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"));

        MockMvcBuilders.standaloneSetup(controller()).build()
                .perform(get("/api/v1/demo/feed").param("userId", "3"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("""
                        {"segment":"beta","postIds":null}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("어노테이션 Span은 기존 요청 Span의 자식으로 생성되고 종료 후 부모로 복원된다")
    void createsChildSpanAndRestoresParent() throws IOException {
        startServer(
                exchange -> respond(exchange, 200, "{\"userId\":7,\"segment\":\"ga\"}"),
                exchange -> respond(exchange, 200, FAST_RANK));
        FeedDemoController controller = controller();
        Span parent = openTelemetry.getTracer("test").spanBuilder("http-request").startSpan();
        try (Scope ignored = parent.makeCurrent()) {
            controller.feed(7L);

            SpanData child = endedSpan();
            assertThat(child.getName()).isEqualTo("recommend-fetch");
            assertThat(child.getParentSpanId()).isEqualTo(parent.getSpanContext().getSpanId());
            assertThat(child.getTraceId()).isEqualTo(parent.getSpanContext().getTraceId());
            assertThat(child.getAttributes().get(AttributeKey.longKey("user.id"))).isEqualTo(7L);
            assertThat(Span.current().getSpanContext()).isEqualTo(parent.getSpanContext());
        } finally {
            parent.end();
        }
    }

    private FeedDemoController controller() {
        RecommendProperties properties = new RecommendProperties(
                "http://localhost:" + server.getAddress().getPort(),
                TIMEOUT,
                TIMEOUT
        );
        RestClient restClient = new RecommendConfig()
                .recommendRestClient(RestClient.builder(), ClientHttpRequestFactoryBuilder.detect(),
                        HttpClientSettings.defaults(), properties);

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().addSpanProcessor(spans).build())
                .build();

        context = new AnnotationConfigApplicationContext();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        context.getEnvironment().getPropertySources()
                .addLast(new PropertiesPropertySource("application", yaml.getObject()));
        context.registerBean(io.micrometer.tracing.Tracer.class,
                () -> new OtelTracer(openTelemetry.getTracer("sns-app.feed-demo"),
                        new OtelCurrentTraceContext(), event -> {}));
        context.registerBean(RecommendClient.class, () -> new RecommendClient(restClient));
        context.registerBean(RecommendProperties.class, () -> properties);
        context.register(AopAutoConfiguration.class, MicrometerTracingAutoConfiguration.class,
                FeedDemoController.class);
        context.refresh();
        return context.getBean(FeedDemoController.class);
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
