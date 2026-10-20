package com.example.backend.dto.game;

import jakarta.validation.constraints.NotNull;

/** Запрос на установку/снятие лайка комментария LLM (ТЗ 7, B2). */
public record LikedRequest(
        @NotNull Boolean liked
) {
}
