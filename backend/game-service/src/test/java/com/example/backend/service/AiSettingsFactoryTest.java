package com.example.backend.service;

import com.example.backend.config.AiProperties;
import com.example.backend.dto.game.Difficulty;
import com.example.backend.error.ApiErrorCode;
import com.example.backend.error.ApiException;
import com.example.backend.game.ai.AiSettings;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Маппинг {@code Difficulty → AiSettings} (ТЗ 6.1): значения пресетов
 * берутся из env ({@code AI_PRESET_*}), {@code CUSTOM} — из запроса (R-3/R-4).
 */
class AiSettingsFactoryTest {

    private final AiSettingsFactory factory = new AiSettingsFactory(new AiProperties(4096, Map.of(
            "easy", new AiProperties.PresetSettings(3, 400),
            "medium", new AiProperties.PresetSettings(4, 1500),
            "hard", new AiProperties.PresetSettings(6, 3000))));

    @Test
    void presetUsesConfiguredDepthAndBudget() {
        AiSettings easy = factory.resolve(Difficulty.EASY, 9, 4999);

        assertEquals(3, easy.maxDepth(), "значения пресета важнее полей запроса");
        assertEquals(400, easy.timeBudgetMs());
        assertEquals(4096, easy.ttCapacity());
    }

    @Test
    void customUsesRequestedValues() {
        AiSettings custom = factory.resolve(Difficulty.CUSTOM, 7, 2500);

        assertEquals(7, custom.maxDepth());
        assertEquals(2500, custom.timeBudgetMs());
        assertEquals(4096, custom.ttCapacity());
    }

    @Test
    void customWithoutValuesIsValidationError() {
        ApiException exception = assertThrows(ApiException.class,
                () -> factory.resolve(Difficulty.CUSTOM, null, null));
        assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.code());
    }

    @Test
    void customOutOfRangeIsValidationError() {
        ApiException exception = assertThrows(ApiException.class,
                () -> factory.resolve(Difficulty.CUSTOM, 20, 2500));
        assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.code());
    }

    @Test
    void missingPresetIsConfigurationError() {
        AiSettingsFactory incomplete = new AiSettingsFactory(new AiProperties(1024, Map.of()));
        assertThrows(IllegalStateException.class,
                () -> incomplete.resolve(Difficulty.HARD, null, null));
    }
}
