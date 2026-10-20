package com.example.backend.game;

import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Emotion;

/**
 * Комментарий LLM к ходу ИИ (ТЗ 4.1, 7.x).
 * {@code id} — идентификатор комментария в таблице {@code llm_comments} (null до сохранения).
 * {@code fallback} — комментарий выдан без обращения к LLM (недоступна/таймаут).
 * {@code style} — стиль комментария (см. {@link CommentaryStyle}).
 * {@code emotion} — эмоциональная окраска комментария (см. {@link Emotion}).
 * {@code liked} — поставлен ли лайк (ТЗ 7, B2).
 */
public record LlmComment(
        Long id,
        long moveNumber,
        String text,
        String model,
        CommentaryStyle style,
        Emotion emotion,
        boolean fallback,
        boolean liked
) {
}
