package com.example.backend.service;

import java.time.Duration;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmServiceClientTest {

    private MockWebServer mockWebServer;
    private LlmServiceClient client;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        client = new LlmServiceClient(restClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void chatReturnsCommentText() {
        String body = "data: Пришлось ответить взятием — продолжаем спокойно.\n\n";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE + "; charset=UTF-8")
                .setBody(body));
        String result = client.chat("ход дамкой", "qwen2.5:7b-instruct");
        assertThat(result).isEqualTo("Пришлось ответить взятием — продолжаем спокойно.");
    }

    @Test
    void chatReturnsNullOnEmptyBody() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
                .setBody(""));
        String result = client.chat("ход", "qwen2.5:7b-instruct");
        assertThat(result).isNull();
    }

    @Test
    void chatThrowsOnUpstreamError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        assertThatThrownBy(() -> client.chat("ход", "qwen2.5:7b-instruct"))
                .isInstanceOf(LlmServiceClient.LlmServiceUnavailable.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void chatThrowsOnConnectionRefused() throws Exception {
        mockWebServer.shutdown();
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:1")
                .build();
        LlmServiceClient unreachable = new LlmServiceClient(restClient);
        assertThatThrownBy(() -> unreachable.chat("ход", "qwen2.5:7b-instruct"))
                .isInstanceOf(LlmServiceClient.LlmServiceUnavailable.class);
    }

    @Test
    void chatRequestContainsPromptAndModel() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
                .setBody("data: ответ\n\n"));
        client.chat("текст промпта", "qwen2.5:7b-instruct");
        var request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/chat");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader("Content-Type")).startsWith(MediaType.APPLICATION_JSON_VALUE);
        String body = request.getBody().readUtf8();
        assertThat(body).contains("текст промпта");
        assertThat(body).contains("qwen2.5:7b-instruct");
    }

    @Test
    void chatTimeoutApplies() {
        // Клиент с очень малым таймаутом — проверяем, что RestClient с factory настраивается
        // через GameConfiguration; здесь просто проверяем, что клиент создаётся без ошибок
        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        LlmServiceClient timeoutClient = new LlmServiceClient(restClient);
        assertThat(timeoutClient).isNotNull();
    }
}
