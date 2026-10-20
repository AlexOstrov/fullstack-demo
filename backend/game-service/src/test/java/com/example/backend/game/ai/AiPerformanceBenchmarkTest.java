package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.game.ai.AiSettings;
import org.junit.jupiter.api.Test;

/**
 * Профилирование производительности ИИ (Sprint 8, R-7 из B4).
 * Измеряет время поиска на начальной позиции для разных пресетов сложности,
 * чтобы убедиться, что поиск укладывается в бюджет времени.
 */
class AiPerformanceBenchmarkTest {

    @Test
    void aiMoveWithinTimeBudget() {
        Board board = Board.initialPosition();

        testPreset(Color.WHITE, new AiSettings(2, 500, AiSettings.DEFAULT_TT_CAPACITY), "EASY");
        testPreset(Color.WHITE, new AiSettings(4, 1500, AiSettings.DEFAULT_TT_CAPACITY), "MEDIUM");
        testPreset(Color.WHITE, new AiSettings(6, 3000, AiSettings.DEFAULT_TT_CAPACITY), "HARD");
        testPreset(Color.WHITE, new AiSettings(8, 2000, AiSettings.DEFAULT_TT_CAPACITY), "CUSTOM(8/2000)");
        testPreset(Color.WHITE, new AiSettings(10, 3000, AiSettings.DEFAULT_TT_CAPACITY), "CUSTOM(10/3000)");
    }

    private void testPreset(Color color, AiSettings settings, String name) {
        AiResult result = new AlphaBetaSearch().findBestMove(
            Board.initialPosition(), color,
            java.util.Collections.emptyMap(), 0, settings
        );
        long elapsedMs = result.elapsedMs();

        System.out.printf(
            "AI preset %s: depth=%d, budget=%dms, elapsed=%dms, move=%s, score=%d, nodes=%d%n",
            name, settings.maxDepth(), settings.timeBudgetMs(),
            elapsedMs, result.move(), result.score(), result.nodes()
        );

        // Поиск должен укладываться в бюджет времени (с допуском 20%)
        long budgetWithTolerance = settings.timeBudgetMs() * 120 / 100;
        if (elapsedMs > budgetWithTolerance) {
            throw new AssertionError(
                String.format("Пресет %s превысил бюджет времени: elapsed=%dms, budget=%dms (с допуском %dms)",
                    name, elapsedMs, settings.timeBudgetMs(), budgetWithTolerance));
        }
    }
}
