package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Настройки ИИ (ТЗ 6, 6.1). Пресеты конфигурируемы через env
 * {@code AI_PRESET_EASY_DEPTH}, {@code AI_PRESET_EASY_TIME_BUDGET_MS} и т. п.;
 * вместимость транспозиционной таблицы — {@code AI_TT_CAPACITY}.
 *
 * @param ttCapacity вместимость транспозиционной таблицы (строк)
 * @param preset     пресеты сложности по ключу {@code easy}/{@code medium}/{@code hard}
 */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(int ttCapacity, Map<String, PresetSettings> preset) {

    public AiProperties {
        preset = preset == null ? Map.of() : Map.copyOf(preset);
    }

    /** Настройки одного пресета сложности (ТЗ 6.1). */
    public record PresetSettings(int depth, long timeBudgetMs) {
    }
}
