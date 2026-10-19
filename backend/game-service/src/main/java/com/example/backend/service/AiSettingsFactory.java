package com.example.backend.service;

import com.example.backend.config.AiProperties;
import com.example.backend.dto.game.Difficulty;
import com.example.backend.error.ApiErrorCode;
import com.example.backend.error.ApiException;
import com.example.backend.game.ai.AiSettings;
import org.springframework.stereotype.Service;

/**
 * Маппинг сложности из запроса в настройки поиска (ТЗ 6.1):
 * пресеты {@code EASY}/{@code MEDIUM}/{@code HARD} берутся из env {@code AI_PRESET_*},
 * {@code CUSTOM} собирается из полей запроса (замечание ревью R-4).
 */
@Service
public class AiSettingsFactory {

    private final AiProperties properties;

    public AiSettingsFactory(AiProperties properties) {
        this.properties = properties;
    }

    /** Настройки поиска для сложности партии. */
    public AiSettings resolve(Difficulty difficulty, Integer customDepth, Integer customBudgetMs) {
        return switch (difficulty) {
            case EASY -> preset("easy");
            case MEDIUM -> preset("medium");
            case HARD -> preset("hard");
            case CUSTOM -> custom(customDepth, customBudgetMs);
        };
    }

    private AiSettings preset(String name) {
        AiProperties.PresetSettings preset = properties.preset().get(name);
        if (preset == null) {
            throw new IllegalStateException("Не задан пресет ИИ в конфигурации: ai.preset." + name);
        }
        return new AiSettings(preset.depth(), preset.timeBudgetMs(), properties.ttCapacity());
    }

    private AiSettings custom(Integer depth, Integer budgetMs) {
        if (depth == null || budgetMs == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR,
                    "Для сложности CUSTOM нужно указать aiDepth и aiTimeBudgetMs");
        }
        try {
            return new AiSettings(depth, budgetMs, properties.ttCapacity());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, exception.getMessage());
        }
    }
}
