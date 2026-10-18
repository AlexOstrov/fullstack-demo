package com.example.backend.dto.game;

/** Источник комментария к ходу ИИ (ТЗ 5.2): живой LLM или детерминированный fallback. */
public enum CommentSource {
    LLM,
    FALLBACK
}
