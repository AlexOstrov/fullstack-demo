package com.example.backend.dto.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос на создание партии (ТЗ 5.1).
 * {@code aiDepth}/{@code aiTimeBudgetMs} задаются только для {@link Difficulty#CUSTOM};
 * для пресетов значения приходят из env {@code AI_PRESET_*}.
 */
public record StartGameRequest(
        @NotNull PlayerColor playerColor,
        @NotNull Difficulty difficulty,
        @Min(1) @Max(12) Integer aiDepth,
        @Min(200) @Max(5000) Integer aiTimeBudgetMs,
        @NotNull CommentaryStyle commentaryStyle,
        @NotBlank String llmModel
) {
}
