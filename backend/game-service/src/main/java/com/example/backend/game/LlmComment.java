package com.example.backend.game;

/**
 * Комментарий LLM к ходу ИИ (ТЗ 4.1, 7.x).
 * {@code fallback} — комментарий выдан без обращения к LLM (недоступна/таймаут).
 */
public record LlmComment(
        long moveNumber,
        String text,
        String model,
        boolean fallback
) {
}
