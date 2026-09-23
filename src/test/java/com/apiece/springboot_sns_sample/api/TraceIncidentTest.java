package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.config.recommend.RecommendConfig;
import com.apiece.springboot_sns_sample.api.demo.TraceResponse;
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

class TraceIncidentTestTest {

    private static final Duration TIMEOUT = Duration.ofMillis(200);
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
    @DisplayName("추천 타임아웃은 503으로 응답하고 Span에 오류와 제한 시간을 남긴다")
    void recordsRankTimeout() throws IOException {
        startServer(
                exchange -> {
                    sleep(TIMEOUT.toMillis() * 3);
                    respond(exchange, 200, FAST_RANK);
                });

        ResponseEntity<TraceResponse> response = controller().incident("hello", 3L);

        assertThat(Span.current().getSpanContext().isValid()).isFalse();
        assertThat(response.getStatusCode().value()).isEqualTo(503);

        SpanData span = endedSpan();
        assertThat(span.getName()).isEqualTo("recommend-fetch");
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getEvents()).anySatisfy(event -> assertThat(event.getName()).isEqualTo("exception"));
        assertThat(span.getAttributes().get(TIMEOUT_MS)).isEqualTo(TIMEOUT.toMillis());
    }

    @Test
    @DisplayName("장애 분석는 추천 API를 한 번만 호출한다")
    void callsRankOnce() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        startServer(exchange -> {
            calls.incrementAndGet();
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            respond(exchange, 200, FAST_RANK);
        });

        assertThat(controller().incident("hello", 7L).getStatusCode().value()).isEqualTo(200);
        assertThat(calls).hasValue(1);
        assertThat(spans.ended).hasSize(1);
    }

    @Test
    @DisplayName("장애 분석 성공 응답은 rankedPostIds만 포함한다")
    void returnsOnlyFeedFieldsOnSuccess() throws Exception {
        startServer(
                exchange -> respond(exchange, 200, FAST_RANK));

        MockMvcBuilders.standaloneSetup(controller()).build()
                .perform(get("/api/v1/demo/trace").param("scenario", "incident"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"message":"hello","rankedPostIds":[101]}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("장애 분석 실패 응답은 null rankedPostIds와 HTTP 503을 반환한다")
    void returnsNullPostIdsOnFailure() throws Exception {
        startServer(
                exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"));

        MockMvcBuilders.standaloneSetup(controller()).build()
                .perform(get("/api/v1/demo/trace").param("scenario", "incident").param("userId", "3"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("""
                        {"message":"hello","rankedPostIds":null}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("어노테이션 Span은 기존 요청 Span의 자식으로 생성되고 종료 후 부모로 복원된다")
    void createsChildSpanAndRestoresParent() throws IOException {
        startServer(
                exchange -> respond(exchange, 200, FAST_RANK));
        ObservabilityDemoController controller = controller();
        Span parent = openTelemetry.getTracer("test").spanBuilder("http-request").startSpan();
        try (Scope ignored = parent.makeCurrent()) {
            controller.incident("hello", 7L);

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

    @Test
    void defaultTraceKeepsResponseAndDoesNotCreateIncidentSpan() throws Exception {
        startServer(exchange -> respond(exchange, 200, FAST_RANK));
        MockMvcBuilders.standaloneSetup(controller()).build()
                .perform(get("/api/v1/demo/trace").param("message", "part-7"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"message":"part-7","rankedPostIds":[101]}
                        """, JsonCompareMode.STRICT));
        assertThat(spans.ended).isEmpty();
    }

    @Test
    void unknownScenarioIsRejectedWithoutCallingRecommendation() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        startServer(exchange -> {
            calls.incrementAndGet();
            respond(exchange, 200, FAST_RANK);
        });
        MockMvcBuilders.standaloneSetup(controller()).build()
                .perform(get("/api/v1/demo/trace").param("scenario", "typo"))
                .andExpect(status().isBadRequest());
        assertThat(calls).hasValue(0);
        assertThat(spans.ended).isEmpty();
    }

    @Test
    void defaultTraceStillPropagatesRecommendationErrors() throws Exception {
        startServer(exchange -> respond(exchange, 500, "{}"));
        var mvc = MockMvcBuilders.standaloneSetup(controller()).build();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mvc.perform(get("/api/v1/demo/trace")))
                .hasCauseInstanceOf(org.springframework.web.client.RestClientException.class);
        assertThat(spans.ended).isEmpty();
    }

    @Test
    void feedMappingIsRemoved() throws Exception {
        startServer(exchange -> respond(exchange, 200, FAST_RANK));
        MockMvcBuilders.standaloneSetup(controller()).build()
                .perform(get("/api/v1/demo/feed"))
                .andExpect(status().isNotFound());
    }

    private ObservabilityDemoController controller() {
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
                () -> new OtelTracer(openTelemetry.getTracer("sns-app.trace-incident"),
                        new OtelCurrentTraceContext(), event -> {}));
        context.registerBean(RecommendClient.class, () -> new RecommendClient(restClient));
        context.registerBean(RecommendProperties.class, () -> properties);
        context.registerBean(com.apiece.springboot_sns_sample.domain.recommend.RecommendService.class);
        context.register(AopAutoConfiguration.class, MicrometerTracingAutoConfiguration.class,
                ObservabilityDemoController.class);
        context.refresh();
        return context.getBean(ObservabilityDemoController.class);
    }

    private SpanData endedSpan() {
        assertThat(spans.ended).hasSize(1);
        return spans.ended.getFirst();
    }

    private void startServer(HttpHandler rankHandler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
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
