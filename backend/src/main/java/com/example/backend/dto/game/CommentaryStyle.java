package com.example.backend.dto.game;

/**
 * Стиль комментария LLM, выбираемый в UI (ТЗ 5.1, 7.4).
 * {@link #token()} — строчный токен для контракта `llm-service` (ТЗ 7.3).
 */
public enum CommentaryStyle {
    FRIENDLY_MASTER("friendly_master"),
    COMPETITIVE("competitive"),
    NEUTRAL("neutral");

    private final String token;

    CommentaryStyle(String token) {
        this.token = token;
    }

    /** Строчный токен стиля для LLM-контракта (ТЗ 7.3/7.4). */
    public String token() {
        return token;
    }
}
