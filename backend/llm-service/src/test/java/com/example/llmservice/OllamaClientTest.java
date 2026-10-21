package com.example.llmservice;

import java.time.Duration;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaClientTest {

    private MockWebServer mockWebServer;
    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        OllamaProperties properties = new OllamaProperties(
                mockWebServer.url("/").toString(),
                Duration.ofSeconds(5),
                Duration.ofSeconds(30),
                Duration.ofSeconds(3)
        );
        ollamaClient = new OllamaClient(WebClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void pingReturnsTrueWhenOllamaReachable() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"models\":[]}"));
        assertThat(ollamaClient.ping().block()).isTrue();
    }

    @Test
    void pingReturnsFalseWhenOllamaUnreachable() {
        OllamaClient unreachableClient = new OllamaClient(
                WebClient.builder(),
                new OllamaProperties(
                        "http://localhost:1",
                        Duration.ofMillis(100),
                        Duration.ofMillis(100),
                        Duration.ofMillis(100)
                )
        );
        assertThat(unreachableClient.ping().block()).isFalse();
    }

    @Test
    void listModelsReturnsModelNames() {
        String body = """
                {"models": [
                    {"name": "qwen2.5:7b-instruct"},
                    {"name": "llama3.1:8b-instruct"},
                    {"name": "unknown-model"}
                ]}
                """;
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(body));
        List<String> models = ollamaClient.listModels().block();
        assertThat(models).containsExactly("qwen2.5:7b-instruct", "llama3.1:8b-instruct", "unknown-model");
    }

    @Test
    void listModelsReturnsEmptyOnUpstreamError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        List<String> models = ollamaClient.listModels().block();
        assertThat(models).isEmpty();
    }

    @Test
    void chatStreamReturnsContentTokens() {
        String body = "{\"message\": {\"content\": \"Hello\"}, \"done\": false}\n{\"message\": {\"content\": \" world\"}, \"done\": true}\n";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/x-ndjson")
                .setBody(body));
        var tokens = ollamaClient.chatStream("qwen2.5:7b-instruct", "test prompt")
                .collectList()
                .block();
        assertThat(tokens).containsExactly("Hello", " world");
    }

    @Test
    void chatStreamSendsMessagesArray() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/x-ndjson")
                .setBody("{\"message\": {\"content\": \"ok\"}, \"done\": true}\n"));
        ollamaClient.chatStream("qwen2.5:7b-instruct", "текст промпта").blockLast();
        var request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        // /api/chat в Ollama принимает только messages; поле prompt игнорируется (пустой ответ)
        assertThat(body).contains("\"messages\"").contains("\"role\":\"user\"").contains("текст промпта");
        assertThat(body).doesNotContain("\"prompt\"");
    }
}
