package com.example.backend.game.ai;

/**
 * Пресеты сложности ИИ (ТЗ 6.1): глубина и бюджет времени по умолчанию
 * для `MEDIUM`/`HARD`; `EASY` фиксирован. `CUSTOM` (глубина 1–12,
 * бюджет 200–5000 мс) собирается напрямую через {@link AiSettings}.
 */
public enum AiPreset {

    /** Лёгкий: глубина 2, бюджет 500 мс. */
    EASY(2, 500),
    /** Средний: глубина 4, бюджет 1500 мс. */
    MEDIUM(4, 1500),
    /** Сложный: глубина 6, бюджет 3000 мс. */
    HARD(6, 3000);

    private final int depth;
    private final long timeBudgetMs;

    AiPreset(int depth, long timeBudgetMs) {
        this.depth = depth;
        this.timeBudgetMs = timeBudgetMs;
    }

    /** Настройки поиска для пресета. */
    public AiSettings settings() {
        return new AiSettings(depth, timeBudgetMs, AiSettings.DEFAULT_TT_CAPACITY);
    }

    public int depth() {
        return depth;
    }

    public long timeBudgetMs() {
        return timeBudgetMs;
    }
}
