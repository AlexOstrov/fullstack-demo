package com.example.llmservice.controller;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.llmservice.LlmChatClient;
import com.example.llmservice.LlmProperties;
import com.example.llmservice.dto.ChatRequest;
import com.example.llmservice.model.ModelCatalog;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmServiceControllerTest {

    private static final String MODEL = "qwen2.5:7b-instruct";

    private final ModelCatalog modelCatalog = mock(ModelCatalog.class);
    private final LlmChatClient chatClient = mock(LlmChatClient.class);
    private final LlmProperties properties =
            new LlmProperties(MODEL, List.of(MODEL), Duration.ofMinutes(5));
    private final LlmServiceController controller =
            new LlmServiceController(modelCatalog, chatClient, properties);

    @Test
    void chatJoinsTokensIntoSingleEvent() {
        when(chatClient.streamChat(MODEL, "промпт"))
                .thenReturn(Flux.just("Интересный", " ход", " белыми."));

        StepVerifier.create(controller.chat(new ChatRequest("промпт", MODEL)))
                .expectNext("Интересный ход белыми.")
                .verifyComplete();
    }

    @Test
    void chatEmitsNothingWhenUpstreamReturnedNoTokens() {
        when(chatClient.streamChat(MODEL, "промпт")).thenReturn(Flux.empty());

        StepVerifier.create(controller.chat(new ChatRequest("промпт", MODEL)))
                .verifyComplete();
    }

    @Test
    void chatUsesDefaultModelWhenRequestHasNoModel() {
        when(chatClient.streamChat(MODEL, "промпт")).thenReturn(Flux.just("ответ"));

        StepVerifier.create(controller.chat(new ChatRequest("промпт", null)))
                .expectNext("ответ")
                .verifyComplete();
    }
}
