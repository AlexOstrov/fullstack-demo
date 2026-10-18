package com.example.backend.controller;

import com.example.backend.dto.game.LlmModelsResponse;
import com.example.backend.service.LlmModelCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Конфигурация для UI (ТЗ 5, базовый путь {@code /api/config}). */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final LlmModelCatalog llmModelCatalog;

    public ConfigController(LlmModelCatalog llmModelCatalog) {
        this.llmModelCatalog = llmModelCatalog;
    }

    /** Предустановленный список моделей LLM (ТЗ 7.2). */
    @GetMapping("/llm-models")
    public LlmModelsResponse llmModels() {
        return llmModelCatalog.response();
    }
}
