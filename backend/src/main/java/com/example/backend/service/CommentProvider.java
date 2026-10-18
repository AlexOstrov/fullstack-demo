package com.example.backend.service;

import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.session.GameSession;

/**
 * Источник комментария к ходу ИИ (ТЗ 7).
 * В Sprint 3 — только детерминированный fallback; Sprint 4 добавит живой LLM
 * с таймаутом и этим же fallback при недоступности.
 */
public interface CommentProvider {

    /** Комментарий к ходу ИИ {@code aiMove} в партии {@code session}. */
    LlmComment comment(GameSession session, Move aiMove);
}
