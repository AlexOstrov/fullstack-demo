package com.example.llmservice;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmChatClientTest {

    private static final OllamaProperties STUB_PROPERTIES = new OllamaProperties(
            "http://localhost:11434",
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            Duration.ofSeconds(3)
    );

    private static OllamaClient ollamaClientStub(Flux<String> response) {
        return new OllamaClient(WebClient.builder(), STUB_PROPERTIES) {
            @Override
            public Flux<String> chatStream(String model, String prompt) {
                return response;
            }

            @Override
            public Mono<Boolean> ping() {
                return Mono.just(true);
            }
        };
    }

    @Test
    void streamChatUsesDefaultModel() {
        OllamaClient ollamaClient = ollamaClientStub(Flux.just("token1", "token2"));
        LlmProperties properties = new LlmProperties(
                "qwen2.5:7b-instruct",
                List.of("qwen2.5:7b-instruct"),
                Duration.ofMinutes(5)
        );
        LlmChatClient client = new LlmChatClient(ollamaClient, properties);
        var tokens = client.streamChat("test prompt").collectList().block();
        assertThat(tokens).containsExactly("token1", "token2");
    }

    @Test
    void streamChatRejectsDisallowedModel() {
        OllamaClient ollamaClient = ollamaClientStub(Flux.empty());
        LlmProperties properties = new LlmProperties(
                "qwen2.5:7b-instruct",
                List.of("qwen2.5:7b-instruct"),
                Duration.ofMinutes(5)
        );
        LlmChatClient client = new LlmChatClient(ollamaClient, properties);
        Flux<String> flux = client.streamChat("unknown-model", "test prompt");
        assertThatThrownBy(() -> flux.collectList().block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model not allowed");
    }

    @Test
    void healthCheckDelegatesToOllamaClient() {
        OllamaClient ollamaClient = ollamaClientStub(Flux.empty());
        LlmProperties properties = new LlmProperties(
                "qwen2.5:7b-instruct",
                List.of("qwen2.5:7b-instruct"),
                Duration.ofMinutes(5)
        );
        LlmChatClient client = new LlmChatClient(ollamaClient, properties);
        assertThat(client.healthCheck().block()).isTrue();
    }
}
