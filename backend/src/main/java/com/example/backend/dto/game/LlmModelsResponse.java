package com.example.backend.dto.game;

import java.util.List;

/** Предустановленный список моделей LLM для UI (ТЗ 5, 7.2). */
public record LlmModelsResponse(List<String> models, String defaultModel) {
}
