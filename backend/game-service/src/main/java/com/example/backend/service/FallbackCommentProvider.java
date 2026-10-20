package com.example.backend.service;

import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Emotion;
import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.game.MoveType;
import com.example.backend.session.GameSession;

/**
 * Детерминированный fallback-комментарий (ТЗ 7.6, 5.2: {@code commentSource = FALLBACK}).
 * Используется, пока LLM недоступен (Sprint 3 — LLM ещё не подключён; Sprint 4 —
 * при таймауте/сбое/выключенном {@code LLM_ENABLED}). Текст не подсказывает ходы
 * и не нарушать правил. Стиль и эмоция берутся из настроек партии.
 * Создаётся как bean в {@link com.example.backend.config.GameConfiguration}.
 */
public class FallbackCommentProvider implements CommentProvider {

    @Override
    public LlmComment comment(GameSession session, Move aiMove) {
        CommentaryStyle style = session.preferences().commentaryStyle();
        Emotion emotion = emotionOf(style);
        String text = text(style, aiMove.type());
        return new LlmComment(null, session.state().moveNumber(), text, null, style, emotion, true, true);
    }

    /** Определить эмоциональную окраску комментария по стилю. */
    private static Emotion emotionOf(CommentaryStyle style) {
        return switch (style) {
            case FRIENDLY_MASTER -> Emotion.NEUTRAL;
            case COMPETITIVE -> Emotion.CONFIDENT;
            case NEUTRAL -> Emotion.NEUTRAL;
        };
    }

    private String text(CommentaryStyle style, MoveType type) {
        return switch (style) {
            case FRIENDLY_MASTER -> switch (type) {
                case CAPTURE -> "Пришлось ответить взятием — продолжаем спокойно.";
                case PROMOTION -> "Моя шашка вышла в дамки, но партия ещё длинная.";
                case NORMAL -> "Делаю осторожный ход, посмотрим на твои планы.";
            };
            case COMPETITIVE -> switch (type) {
                case CAPTURE -> "Забираю! Посмотрим, как ты выкрутишься.";
                case PROMOTION -> "А вот и дамка. Придётся тебе попотеть.";
                case NORMAL -> "Мой ход. Интересно, что ты придумаешь.";
            };
            case NEUTRAL -> switch (type) {
                case CAPTURE -> "Выполнено взятие.";
                case PROMOTION -> "Шашка превращена в дамку.";
                case NORMAL -> "Ход сделан.";
            };
        };
    }
}
