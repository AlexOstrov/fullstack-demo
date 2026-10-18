package com.example.backend.service;

import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.game.MoveType;
import com.example.backend.session.GameSession;
import org.springframework.stereotype.Service;

/**
 * Детерминированный fallback-комментарий (ТЗ 7.6, 5.2: {@code commentSource = FALLBACK}).
 * Используется, пока LLM недоступен (Sprint 3 — LLM ещё не подключён; Sprint 4 —
 * при таймауте/сбое/выключенном {@code LLM_ENABLED}). Текст не подсказывает ходы
 * и не нарушает правил. Стиль берётся из настроек партии.
 */
@Service
public class FallbackCommentProvider implements CommentProvider {

    @Override
    public LlmComment comment(GameSession session, Move aiMove) {
        CommentaryStyle style = session.preferences().commentaryStyle();
        String text = text(style, aiMove.type());
        return new LlmComment(session.state().moveNumber(), text, null, true);
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
