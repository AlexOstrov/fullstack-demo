package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Настройки LLM-сервиса (ТЗ 7.2). Env: {@code OLLAMA_BASE_URL}, {@code LLM_MODEL},
 * {@code LLM_ALLOWED_MODELS}, {@code LLM_TIMEOUT_MS}, {@code LLM_MAX_RETRIES},
 * {@code LLM_TEMPERATURE}, {@code LLM_NUM_CTX}, {@code LLM_ENABLED}.
 * <p>Sprint 3 использует только {@code model}/{@code allowedModels} — для
 * {@code GET /api/config/llm-models} и allowlist; вызовы Ollama — Sprint 4.
 *
 * @param baseUrl       адрес Ollama (через {@code llm-service})
 * @param model         модель по умолчанию
 * @param allowedModels allowlist моделей для селектора в UI
 * @param timeoutMs     таймаут запроса к LLM
 * @param maxRetries    число повторов при сбое
 * @param temperature   температура генерации
 * @param numCtx        размер контекста
 * @param enabled       включён ли живой LLM (иначе — только fallback)
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String baseUrl,
        String model,
        List<String> allowedModels,
        long timeoutMs,
        int maxRetries,
        double temperature,
        int numCtx,
        boolean enabled
) {

    public LlmProperties {
        allowedModels = allowedModels == null ? List.of() : List.copyOf(allowedModels);
    }
}
