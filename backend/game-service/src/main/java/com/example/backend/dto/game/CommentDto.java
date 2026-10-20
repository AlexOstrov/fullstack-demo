package com.example.backend.dto.game;

import com.example.backend.game.LlmComment;

/** Комментарий LLM в истории (ТЗ 5.2, 7.5). */
public record CommentDto(Long id, long moveNumber, String text, String model, CommentSource commentSource,
                         boolean liked) {

    public static CommentDto from(LlmComment comment) {
        return new CommentDto(comment.id(), comment.moveNumber(), comment.text(), comment.model(),
                comment.fallback() ? CommentSource.FALLBACK : CommentSource.LLM,
                comment.liked());
    }
}
