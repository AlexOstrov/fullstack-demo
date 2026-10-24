package com.example.backend.service;

import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Emotion;
import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.game.MoveType;
import com.example.backend.model.LlmCommentEntity;
import com.example.backend.repository.LlmCommentRepository;
import com.example.backend.session.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * Fallback-комментарий к ходу ИИ (ТЗ 7.6, 5.2: {@code commentSource = FALLBACK}).
 * <p>Используется при выключенном {@code LLM_ENABLED}, недоступности, таймауте
 * или пустом ответе LLM. Сначала берётся случайный сохранённый комментарий
 * того же стиля из {@code llm_comments} (B3) — так игрок видит живые реплики,
 * а не одну и ту же фразу. Если подходящих комментариев нет или БД недоступна,
 * выдаётся детерминированный текст. Стиль и эмоция берутся из настроек партии.
 * <p>Создаётся как bean в {@link com.example.backend.config.GameConfiguration}.
 */
public class FallbackCommentProvider implements CommentProvider {

    private static final Logger log = LoggerFactory.getLogger(FallbackCommentProvider.class);

    private final LlmCommentRepository repository;
    private final CommentTextSanitizer sanitizer;
    private final RandomGenerator random;

    /** Детерминированный провайдер без доступа к сохранённым комментариям (тесты/утилиты). */
    public FallbackCommentProvider() {
        this(null, new CommentTextSanitizer(), new SplittableRandom());
    }

    public FallbackCommentProvider(LlmCommentRepository repository, RandomGenerator random) {
        this(repository, new CommentTextSanitizer(), random);
    }

    public FallbackCommentProvider(LlmCommentRepository repository,
                                   CommentTextSanitizer sanitizer,
                                   RandomGenerator random) {
        this.repository = repository;
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public LlmComment comment(GameSession session, Move aiMove) {
        CommentaryStyle style = session.preferences().commentaryStyle();
        Emotion emotion = emotionOf(style);
        String text = storedComment(style);
        if (text == null) {
            text = text(style, aiMove.type());
        }
        return new LlmComment(null, session.state().moveNumber(), text, null, style, emotion, true, true);
    }

    /** Случайный сохранённый комментарий нужного стиля; {@code null}, если нет/БД недоступна. */
    private String storedComment(CommentaryStyle style) {
        if (repository == null) {
            return null;
        }
        try {
            List<LlmCommentEntity> comments =
                    repository.findByStyleAndLikedTrueAndLlmModelIsNotNull(style.token());
            if (comments.isEmpty()) {
                return null;
            }
            String text = comments.get(random.nextInt(comments.size())).getComment();
            // Старые записи могли сохраниться без фильтра — не показываем иероглифы.
            return sanitizer.sanitize(text).orElse(null);
        } catch (RuntimeException exception) {
            // Best-effort: сбой БД не должен ломать ход ИИ.
            log.warn("Не удалось выбрать сохранённый комментарий (best-effort): {}",
                    exception.getMessage());
            return null;
        }
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
