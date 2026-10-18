package com.example.backend.game.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiSettingsTest {

    @Test
    void presetsProvideDepthAndBudget() {
        AiSettings easy = AiSettings.of(AiPreset.EASY);
        assertEquals(2, easy.maxDepth());
        assertEquals(500, easy.timeBudgetMs());

        AiSettings medium = AiSettings.of(AiPreset.MEDIUM);
        assertEquals(4, medium.maxDepth());
        assertEquals(1500, medium.timeBudgetMs(),
                "MEDIUM — бюджет по умолчанию (ТЗ 6)");

        AiSettings hard = AiSettings.of(AiPreset.HARD);
        assertEquals(6, hard.maxDepth());
        assertEquals(3000, hard.timeBudgetMs());

        assertEquals(AiSettings.DEFAULT_TT_CAPACITY, easy.ttCapacity());
    }

    @Test
    void customSettingsAccepted() {
        AiSettings custom = new AiSettings(12, 200, 64);
        assertEquals(12, custom.maxDepth());
        assertEquals(200, custom.timeBudgetMs());
    }

    @Test
    void depthOutsideRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new AiSettings(0, 1500, 64));
        assertThrows(IllegalArgumentException.class, () -> new AiSettings(13, 1500, 64));
    }

    @Test
    void budgetOutsideRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new AiSettings(4, 199, 64));
        assertThrows(IllegalArgumentException.class, () -> new AiSettings(4, 5001, 64));
    }

    @Test
    void tooSmallTtCapacityThrows() {
        assertThrows(IllegalArgumentException.class, () -> new AiSettings(4, 1500, 32));
    }
}
