package com.example.backend.service;

import com.example.backend.config.LlmProperties;
import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Emotion;
import com.example.backend.game.Board;
import com.example.backend.game.Cell;
import com.example.backend.game.Color;
import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.game.Piece;
import com.example.backend.session.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Источник комментария к ходу ИИ с живым LLM и детерминированным fallback.
 * <p>Сначала пытается запросить комментарий у {@code llm-service} (прокси к Ollama).
 * При недоступности, таймауте, выключенном {@code LLM_ENABLED} или непригодном
 * ответе (иероглифы, не русский язык, пустой текст) — используется
 * {@link FallbackCommentProvider}.
 * <p>Ответ проходит {@link CommentTextSanitizer}: убираются markdown/кавычки,
 * отбрасываются иноязычные вставки (B5 из бэклога).
 * <p>Создаётся как bean в {@link com.example.backend.config.GameConfiguration}.
 */
public class LlmCommentProvider implements CommentProvider {

    private static final Logger log = LoggerFactory.getLogger(LlmCommentProvider.class);

    private final LlmServiceClient llmClient;
    private final LlmProperties llmProperties;
    private final FallbackCommentProvider fallback;
    private final CommentTextSanitizer sanitizer;

    public LlmCommentProvider(LlmServiceClient llmClient,
                              LlmProperties llmProperties,
                              FallbackCommentProvider fallback) {
        this(llmClient, llmProperties, fallback, new CommentTextSanitizer());
    }

    public LlmCommentProvider(LlmServiceClient llmClient,
                              LlmProperties llmProperties,
                              FallbackCommentProvider fallback,
                              CommentTextSanitizer sanitizer) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
        this.llmProperties = Objects.requireNonNull(llmProperties, "llmProperties");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
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
            String raw = llmClient.chat(prompt, model);
            Optional<String> text = sanitizer.sanitize(raw);
            if (text.isPresent()) {
                long moveNumber = session.state().moveNumber();
                return new LlmComment(null, moveNumber, text.get(), model, style, emotion, false, true);
            }
            log.debug("Ответ LLM отклонён пост-фильтром (иноязычный/пустой): {}", raw);
        } catch (LlmServiceClient.LlmServiceUnavailable ignored) {
            // LLM недоступен — используем fallback (он сам определит style/emotion из сессии)
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
        sb.append("Ты комментатор партии в русские шашки. ")
                .append("Пиши ТОЛЬКО по-русски, без иероглифов, эмодзи и латинских слов.\n\n");
        sb.append("Доска (нотация a1–h8: '.' пусто, 'w'/'W' — белые, 'b'/'B' — чёрные):\n");
        sb.append(boardAsText(session.state().board()));
        sb.append("\nХод ИИ (цвет ").append(aiColor).append("): ")
                .append(moveNotation(aiMove))
                .append(", тип: ").append(aiMove.type()).append('\n');
        sb.append("Номер хода: ").append(moveNumber).append('\n');
        sb.append("Стиль комментария: ").append(style.token())
                .append('(').append(style).append(")\n\n");
        sb.append("Нотация: «c3–d4» — обычный ход, «c3×e5» — взятие. ")
                .append("Используй только такую нотацию; не пиши «строка», «столбец» ")
                .append("и не называй клетки парами чисел.\n");
        sb.append("Напиши короткий комментарий (1–2 предложения) в стиле ").append(style.token())
                .append('.')
                .append(" Не подсказывай ходы, не нарушай правил, не оценивай позицию числово.");
        return sb.toString();
    }

    /** Ход в нотации a1–h8: {@code c3–d4} для тихого хода, {@code c3×e5} для взятия. */
    static String moveNotation(Move move) {
        String separator = isCapture(move) ? "×" : "–";
        StringBuilder sb = new StringBuilder();
        for (Cell cell : move.path()) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(cellName(cell));
        }
        return sb.toString();
    }

    private static boolean isCapture(Move move) {
        for (int index = 1; index < move.path().size(); index++) {
            Cell previous = move.path().get(index - 1);
            Cell next = move.path().get(index);
            if (Math.abs(next.row() - previous.row()) > 1
                    || Math.abs(next.col() - previous.col()) > 1) {
                return true;
            }
        }
        return false;
    }

    private static String cellName(Cell cell) {
        return String.valueOf((char) ('a' + cell.col())) + (Board.SIZE - cell.row());
    }

    private String boardAsText(Board board) {
        StringBuilder sb = new StringBuilder("  a b c d e f g h\n");
        for (int row = 0; row < Board.SIZE; row++) {
            sb.append(Board.SIZE - row).append(' ');
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                char symbol = switch (piece) {
                    case EMPTY -> '.';
                    case WHITE_MAN -> 'w';
                    case WHITE_KING -> 'W';
                    case BLACK_MAN -> 'b';
                    case BLACK_KING -> 'B';
                };
                sb.append(symbol);
                if (col < Board.SIZE - 1) {
                    sb.append(' ');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
