package com.example.llmservice.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.llmservice.LlmChatClient;
import com.example.llmservice.LlmProperties;
import com.example.llmservice.dto.ChatRequest;
import com.example.llmservice.dto.ModelsResponse;
import com.example.llmservice.model.ModelCatalog;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class LlmServiceController {

    private final ModelCatalog modelCatalog;
    private final LlmChatClient chatClient;
    private final LlmProperties properties;

    public LlmServiceController(ModelCatalog modelCatalog, LlmChatClient chatClient, LlmProperties properties) {
        this.modelCatalog = modelCatalog;
        this.chatClient = chatClient;
        this.properties = properties;
    }

    @GetMapping("/models")
    public ModelsResponse models() {
        List<String> models = modelCatalog.getCachedModels();
        String defaultModel = models.contains(properties.model()) ? properties.model() :
                (models.isEmpty() ? properties.model() : models.get(0));
        return new ModelsResponse(models, defaultModel);
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@Valid @RequestBody ChatRequest request) {
        String model = request.model() != null ? request.model() : properties.model();
        return chatClient.streamChat(model, request.prompt());
    }
}
