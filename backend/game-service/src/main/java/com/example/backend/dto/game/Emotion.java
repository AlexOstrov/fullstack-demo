package com.example.backend.dto.game;

/**
 * Эмоциональная окраска комментария LLM к ходу ИИ (ТЗ 7.4).
 * Позволяет UI подобрать подходящую иконку или оформление комментария.
 */
public enum Emotion {
    NEUTRAL("neutral"),
    EXCITED("excited"),
    CONFIDENT("confident"),
    CAUTIOUS("cautious");

    private final String token;

    Emotion(String token) {
        this.token = token;
    }

    /** Строчный токен эмоции для контракта LLM/ UI (ТЗ 7.4). */
    public String token() {
        return token;
    }
}
