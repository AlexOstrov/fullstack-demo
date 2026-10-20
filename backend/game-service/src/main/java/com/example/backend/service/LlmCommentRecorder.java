package com.example.backend.service;

import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Emotion;
import com.example.backend.game.LlmComment;
import com.example.backend.model.LlmCommentEntity;
import com.example.backend.repository.LlmCommentRepository;
import com.example.backend.session.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Автосохранение комментария LLM к ходу ИИ в {@code llm_comments} (ТЗ 7, B2).
 * <p>Best-effort: сбой БД логируется и не ломает партию (ADR-0004).
 * Комментарий сохраняется при каждом ходе ИИ — идемпотентность не требуется,
 * так как каждый комментарий уникален (разный move_number и текст).
 */
@Service
public class LlmCommentRecorder {

    private static final Logger log = LoggerFactory.getLogger(LlmCommentRecorder.class);

    private final LlmCommentRepository repository;

    public LlmCommentRecorder(LlmCommentRepository repository) {
        this.repository = repository;
    }

    /**
     * Сохранить комментарий ИИ в БД. Сбой БД не пробрасывается — партия не ломается.
     *
     * @param session партия (нужен gameId)
     * @param comment комментарий LLM (из {@link LlmCommentProvider})
     */
    public void record(GameSession session, LlmComment comment) {
        try {
            repository.save(toEntity(session, comment));
            log.info("Комментарий ИИ партии {} сохранён (ход {}, fallback={})",
                    session.id(), comment.moveNumber(), comment.fallback());
        } catch (RuntimeException exception) {
            log.warn("Не удалось сохранить комментарий ИИ партии {} (best-effort): {}",
                    session.id(), exception.getMessage());
        }
    }

    private LlmCommentEntity toEntity(GameSession session, LlmComment comment) {
        return new LlmCommentEntity(
                session.id(),
                comment.text(),
                comment.style() != null ? comment.style().token() : null,
                comment.emotion() != null ? comment.emotion().token() : null,
                comment.model(),
                (int) comment.moveNumber(),
                Instant.now());
    }
}
