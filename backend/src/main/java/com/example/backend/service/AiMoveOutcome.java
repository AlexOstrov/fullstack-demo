package com.example.backend.service;

import com.example.backend.game.LlmComment;
import com.example.backend.session.GameSession;

/** Результат {@code ai-move}: партия и комментарий к ходу ИИ (ТЗ 5.2). */
public record AiMoveOutcome(GameSession session, LlmComment comment) {
}
