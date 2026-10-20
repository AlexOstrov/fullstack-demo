package com.example.backend.dto.game;

/** Ход ИИ с комментарием (ТЗ 5.2, поле {@code ai} ответа {@code ai-move}). */
public record AiTurnDto(
        MoveDto move,
        String comment,
        String emotion,
        CommentSource commentSource,
        boolean liked
) {
}
