package com.example.llmservice.model;

import java.time.Duration;
import java.util.List;

import com.example.llmservice.LlmProperties;
import com.example.llmservice.OllamaClient;
import com.example.llmservice.OllamaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCatalogTest {

    private static OllamaClient ollamaClientStub(List<String> models) {
        return new OllamaClient(WebClient.builder(),
                new OllamaProperties("http://localhost:11434", Duration.ofSeconds(5),
                        Duration.ofSeconds(30), Duration.ofSeconds(3))) {
            @Override
            public Mono<List<String>> listModels() {
                return Mono.just(models);
            }
        };
    }

    @Test
    void filtersOllamaModelsByAllowlist() {
        OllamaClient ollamaClient = ollamaClientStub(
                List.of("qwen2.5:7b-instruct", "llama3.1:8b-instruct", "unknown-model"));
        LlmProperties properties = new LlmProperties(
                "qwen2.5:7b-instruct",
                List.of("qwen2.5:7b-instruct", "llama3.1:8b-instruct"),
                Duration.ofMinutes(5)
        );
        ModelCatalog catalog = new ModelCatalog(ollamaClient, properties);
        catalog.init();
        assertThat(catalog.getCachedModels()).containsExactly("qwen2.5:7b-instruct", "llama3.1:8b-instruct");
    }

    @Test
    void initCachesAllowedModels() {
        OllamaClient ollamaClient = ollamaClientStub(List.of("qwen2.5:7b-instruct"));
        LlmProperties properties = new LlmProperties(
                "qwen2.5:7b-instruct",
                List.of("qwen2.5:7b-instruct"),
                Duration.ofMinutes(5)
        );
        ModelCatalog catalog = new ModelCatalog(ollamaClient, properties);
        catalog.init();
        assertThat(catalog.getCachedModels()).containsExactly("qwen2.5:7b-instruct");
    }

    @Test
    void getCachedModelsReturnsEmptyListBeforeInit() {
        OllamaClient ollamaClient = ollamaClientStub(List.of());
        LlmProperties properties = new LlmProperties(
                "qwen2.5:7b-instruct",
                List.of("qwen2.5:7b-instruct"),
                Duration.ofMinutes(5)
        );
        ModelCatalog catalog = new ModelCatalog(ollamaClient, properties);
        assertThat(catalog.getCachedModels()).isEmpty();
    }
}
