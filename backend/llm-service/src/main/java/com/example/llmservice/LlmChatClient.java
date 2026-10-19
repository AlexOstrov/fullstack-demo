package com.example.llmservice;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class LlmChatClient {

    private final OllamaClient ollamaClient;
    private final LlmProperties properties;

    public LlmChatClient(OllamaClient ollamaClient, LlmProperties properties) {
        this.ollamaClient = ollamaClient;
        this.properties = properties;
    }

    public Flux<String> streamChat(String prompt) {
        return streamChat(properties.model(), prompt);
    }

    public Flux<String> streamChat(String model, String prompt) {
        if (!properties.isAllowed(model)) {
            return Flux.error(new IllegalArgumentException("Model not allowed: " + model));
        }
        return ollamaClient.chatStream(model, prompt);
    }

    public Mono<Boolean> healthCheck() {
        return ollamaClient.ping();
    }
}
