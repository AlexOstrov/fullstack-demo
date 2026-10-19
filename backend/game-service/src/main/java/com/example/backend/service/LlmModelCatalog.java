package com.example.backend.service;

import com.example.backend.config.LlmProperties;
import com.example.backend.dto.game.LlmModelsResponse;
import com.example.backend.error.ApiErrorCode;
import com.example.backend.error.ApiException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Каталог моделей LLM (ТЗ 5, 7.2): allowlist {@code LLM_ALLOWED_MODELS}
 * и модель по умолчанию {@code LLM_MODEL}. В Sprint 4 список будет
 * пересекаться с {@code GET /api/tags} от Ollama, сейчас — конфигурация.
 */
@Service
public class LlmModelCatalog {

    private final LlmProperties properties;

    public LlmModelCatalog(LlmProperties properties) {
        this.properties = properties;
    }

    /** Модели для селектора в UI. */
    public List<String> models() {
        return properties.allowedModels();
    }

    /** Модель по умолчанию (если конфигурируемая недоступна — первая из списка). */
    public String defaultModel() {
        List<String> models = models();
        if (models.isEmpty()) {
            return properties.model();
        }
        return models.contains(properties.model()) ? properties.model() : models.get(0);
    }

    /** Проверить, что запрошенная модель входит в allowlist (ТЗ 5.1). */
    public void requireAllowed(String model) {
        if (!models().contains(model)) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                    "Модель вне allowlist: " + model + "; доступны: " + models());
        }
    }

    /** Ответ {@code GET /api/config/llm-models}. */
    public LlmModelsResponse response() {
        return new LlmModelsResponse(models(), defaultModel());
    }
}
