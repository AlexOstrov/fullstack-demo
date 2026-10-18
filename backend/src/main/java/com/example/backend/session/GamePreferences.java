package com.example.backend.session;

import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Difficulty;
import com.example.backend.dto.game.PlayerColor;

/**
 * Выбранные игроком настройки партии (ТЗ 5.1).
 * Хранятся в сессии, чтобы {@code restart} мог повторить их, если новый запрос без настроек.
 */
public record GamePreferences(
        PlayerColor playerColor,
        Difficulty difficulty,
        Integer aiDepth,
        Integer aiTimeBudgetMs,
        CommentaryStyle commentaryStyle,
        String llmModel
) {
}
