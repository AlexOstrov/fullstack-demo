package com.example.backend.service;

import com.example.backend.model.LlmCommentEntity;
import com.example.backend.repository.LlmCommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Управление лайками комментариев LLM (ТЗ 7, B2).
 * <p>«Вверх» → комментарий сохраняется/остаётся в {@code llm_comments};
 * «вниз» → комментарий удаляется из {@code llm_comments} (не сохраняется).
 */
@Service
public class LlmCommentService {

    private static final Logger log = LoggerFactory.getLogger(LlmCommentService.class);

    private final LlmCommentRepository repository;

    public LlmCommentService(LlmCommentRepository repository) {
        this.repository = repository;
    }

    /**
     * Поставить или снять лайк с комментария.
     *
     * @param gameId 	id партии
     * @param commentId id комментария
     * @param liked     {@code true} — поставить лайк (оставить в БД),
     *                  {@code false} — снять лайк (удалить из БД)
     * @return {@code true}, если комментарий найден и операция выполнена
     */
    public boolean toggleLike(UUID gameId, Long commentId, boolean liked) {
        return repository.findById(commentId)
                .filter(comment -> comment.getGameId().equals(gameId))
                .map(comment -> {
                    if (liked) {
                        comment.setLiked(true);
                        repository.save(comment);
                        log.info("Лайк поставлен комментарию {} партии {}", commentId, gameId);
                    } else {
                        repository.delete(comment);
                        log.info("Лайк снят с комментария {} партии {}", commentId, gameId);
                    }
                    return true;
                })
                .orElse(false);
    }
}
