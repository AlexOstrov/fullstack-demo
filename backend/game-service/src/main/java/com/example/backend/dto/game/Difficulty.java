package com.example.backend.dto.game;

/**
 * Сложность ИИ, выбираемая в UI (ТЗ 5.1, 6.1).
 * {@code EASY}/{@code MEDIUM}/{@code HARD} — пресеты глубины и бюджета;
 * {@code CUSTOM} — значения приходят в запросе ({@code aiDepth}, {@code aiTimeBudgetMs}).
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD,
    CUSTOM
}
