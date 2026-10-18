package com.example.backend.game.ai;

/**
 * Настройки поиска ИИ (ТЗ 6, 6.1): глубина (1–12 для `CUSTOM`),
 * бюджет времени на ход (200–5000 мс, по умолчанию 1500 — ТЗ 6),
 * вместимость транспозиционной таблицы (настраиваемая, ограничивается).
 */
public record AiSettings(int maxDepth, long timeBudgetMs, int ttCapacity) {

    /** Границы глубины (ТЗ 6.1: `CUSTOM` — 1–12 полуходов). */
    public static final int MIN_DEPTH = 1;
    public static final int MAX_DEPTH = 12;

    /** Границы бюджета времени (ТЗ 6: максимум 5000 мс, `CUSTOM` от 200). */
    public static final long MIN_TIME_BUDGET_MS = 200;
    public static final long MAX_TIME_BUDGET_MS = 5000;

    /** Бюджет времени на ход по умолчанию (ТЗ 6). */
    public static final long DEFAULT_TIME_BUDGET_MS = 1500;

    /** Вместимость транспозиционной таблицы по умолчанию (строк TT). */
    public static final int DEFAULT_TT_CAPACITY = 1 << 16;

    public AiSettings {
        if (maxDepth < MIN_DEPTH || maxDepth > MAX_DEPTH) {
            throw new IllegalArgumentException(
                    "глубина поиска вне диапазона " + MIN_DEPTH + "–" + MAX_DEPTH + ": " + maxDepth);
        }
        if (timeBudgetMs < MIN_TIME_BUDGET_MS || timeBudgetMs > MAX_TIME_BUDGET_MS) {
            throw new IllegalArgumentException(
                    "бюджет времени вне диапазона " + MIN_TIME_BUDGET_MS + "–"
                            + MAX_TIME_BUDGET_MS + " мс: " + timeBudgetMs);
        }
        if (ttCapacity < 64) {
            throw new IllegalArgumentException("ttCapacity должна быть не меньше 64: " + ttCapacity);
        }
    }

    /** Настройки пресета сложности (ТЗ 6.1). */
    public static AiSettings of(AiPreset preset) {
        return preset.settings();
    }
}
