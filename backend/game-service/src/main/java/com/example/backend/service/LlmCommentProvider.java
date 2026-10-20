package com.example.backend.service;

import com.example.backend.config.LlmProperties;
import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Emotion;
import com.example.backend.game.Cell;
import com.example.backend.game.Color;
import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.game.Piece;
import com.example.backend.session.GameSession;
import org.springframework.stereotype.Service;

/**
 * Источник комментария к ходу ИИ с живым LLM и детерминированным fallback.
 * <p>Сначала пытается запросить комментарий у {@code llm-service} (прокси к Ollama).
 * При недоступности, таймауте или выключенном {@code LLM_ENABLED} — используется
 * {@link FallbackCommentProvider}.
 * Успешный ответ помечается {@code fallback = false} с указанием модели, стиля и эмоции.
 */
@Service
public class LlmCommentProvider implements CommentProvider {

    private final LlmServiceClient llmClient;
    private final LlmProperties llmProperties;
    private final FallbackCommentProvider fallback;

    public LlmCommentProvider(LlmServiceClient llmClient,
                              LlmProperties llmProperties,
                              FallbackCommentProvider fallback) {
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
        this.fallback = fallback;
    }

    @Override
    public LlmComment comment(GameSession session, Move aiMove) {
        CommentaryStyle style = session.preferences().commentaryStyle();
        Emotion emotion = emotionOf(style);
        if (!llmProperties.enabled()) {
            return fallback.comment(session, aiMove);
        }
        String model = session.preferences().llmModel() != null
                ? session.preferences().llmModel()
                : llmProperties.model();
        String prompt = buildPrompt(session, aiMove, model, style);
        try {
            String text = llmClient.chat(prompt, model);
            if (text != null && !text.isBlank()) {
                long moveNumber = session.state().moveNumber();
                return new LlmComment(null, moveNumber, text, model, style, emotion, false, true);
            }
        } catch (LlmServiceClient.LlmServiceUnavailable ignored) {
            // ЛLM недоступен — используем fallback (он сам определит style/emotion из сессии)
        }
        return fallback.comment(session, aiMove);
    }

    /** Определить эмоциональную окраску комментария по стилю. */
    private static Emotion emotionOf(CommentaryStyle style) {
        return switch (style) {
            case FRIENDLY_MASTER -> Emotion.NEUTRAL;
            case COMPETITIVE -> Emotion.CONFIDENT;
            case NEUTRAL -> Emotion.NEUTRAL;
        };
    }

    /** Построить промпт для LLM из состояния партии и хода ИИ. */
    private String buildPrompt(GameSession session, Move aiMove, String model, CommentaryStyle style) {
        Color aiColor = session.aiColor();
        long moveNumber = session.state().moveNumber();

        StringBuilder sb = new StringBuilder();
        sb.append("Ты комментатор партии в русские шашки. Отвечай строго на русском языке.\n\n");
        sb.append("Доска (клетки: '.' пусто, 'w'/'W' — белые, 'b'/'B' — чёрные, строки 0–7 сверху вниз):\n");
        sb.append(boardAsText(session.state().board()));
        sb.append("\n\n");
        sb.append("Ход ИИ (цвет ").append(aiColor).append("): ");
        Cell from = aiMove.path().get(0);
        Cell to = aiMove.path().get(aiMove.path().size() - 1);
        sb.append('(').append(from.row()).append(',').append(from.col()).append(") → ")
                .append('(').append(to.row()).append(',').append(to.col()).append(')')
                .append(", тип: ").append(aiMove.type()).append('\n');
        sb.append("Номер хода: ").append(moveNumber).append('\n');
        sb.append("Стиль комментария: ").append(style.token())
                .append('(').append(style).append(")\n\n");
        sb.append("Напиши короткий комментарий (1–2 предложения) в стиле ").append(style.token())
                .append('.')
                .append(" Не подсказывай ходы, не нарушай правил, не оценивай позицию числово.");
        return sb.toString();
    }

    private String boardAsText(com.example.backend.game.Board board) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < com.example.backend.game.Board.SIZE; row++) {
            for (int col = 0; col < com.example.backend.game.Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                char symbol = switch (piece) {
                    case EMPTY -> '.';
                    case WHITE_MAN -> 'w';
                    case WHITE_KING -> 'W';
                    case BLACK_MAN -> 'b';
                    case BLACK_KING -> 'B';
                };
                sb.append(symbol);
                if (col < com.example.backend.game.Board.SIZE - 1) {
                    sb.append(' ');
                }
            }
            sb.append("  (ряд ").append(row).append(')').append('\n');
        }
        return sb.toString();
    }
}
