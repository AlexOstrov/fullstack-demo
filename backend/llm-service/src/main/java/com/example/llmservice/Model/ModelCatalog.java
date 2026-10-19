package com.example.llmservice.model;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.llmservice.LlmProperties;
import com.example.llmservice.OllamaClient;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@Component
public class ModelCatalog {

    private final OllamaClient ollamaClient;
    private final LlmProperties properties;

    private volatile List<String> cachedModels = List.of();

    public ModelCatalog(OllamaClient ollamaClient, LlmProperties properties) {
        this.ollamaClient = ollamaClient;
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        refresh();
    }

    /** Принудительное обновление каталога из Ollama (блокирующе). */
    public void refresh() {
        Mono<List<String>> mono = ollamaClient.listModels();
        if (mono == null) {
            cachedModels = List.of();
            return;
        }
        List<String> models = mono.timeout(Duration.ofSeconds(10)).block();
        if (models != null) {
            cachedModels = models.stream()
                    .filter(properties::isAllowed)
                    .distinct()
                    .toList();
        }
    }

    public List<String> getCachedModels() {
        return cachedModels;
    }
}
