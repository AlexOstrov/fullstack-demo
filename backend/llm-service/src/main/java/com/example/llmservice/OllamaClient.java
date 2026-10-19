package com.example.llmservice;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OllamaClient {

    private final WebClient webClient;
    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaClient(WebClient.Builder webClientBuilder, OllamaProperties properties) {
        this(webClientBuilder, properties, new ObjectMapper());
    }

    OllamaClient(WebClient.Builder webClientBuilder, OllamaProperties properties, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Mono<Boolean> ping() {
        return webClient.get()
                .uri("/api/tags")
                .retrieve()
                .toBodilessEntity()
                .timeout(properties.tagsTimeout())
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }

    public Flux<String> chatStream(String model, String prompt) {
        OllamaChatRequest request = new OllamaChatRequest(model, prompt, true);
        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(properties.chatTimeout())
                .flatMap(line -> {
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        String content = node.path("message").path("content").asText(null);
                        boolean done = node.path("done").asBoolean(false);
                        Flux<String> result = content != null ? Flux.just(content) : Flux.empty();
                        return done ? result : result;
                    } catch (Exception e) {
                        return Flux.empty();
                    }
                });
    }

    public Mono<List<String>> listModels() {
        return webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(properties.tagsTimeout())
                .map(body -> {
                    try {
                        JsonNode root = objectMapper.readTree(body);
                        List<String> names = new ArrayList<String>();
                        JsonNode models = root.path("models");
                        if (models.isArray()) {
                            for (JsonNode model : models) {
                                String name = model.path("name").asText(null);
                                if (name != null) {
                                    names.add(name);
                                }
                            }
                        }
                        return names;
                    } catch (Exception e) {
                        return java.util.Collections.<String>emptyList();
                    }
                })
                .onErrorReturn(java.util.Collections.<String>emptyList());
    }

    public record OllamaChatRequest(String model, String prompt, Boolean stream) {
    }
}
