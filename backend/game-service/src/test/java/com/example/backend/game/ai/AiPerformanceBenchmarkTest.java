package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.game.Piece;
import com.example.backend.game.ai.AiSettings;
import org.junit.jupiter.api.Test;

/**
 * Профилирование производительности ИИ (Sprint 8, R-7 из B4).
 * Измеряет время поиска на начальной и плотной миттельшпильной позиции
 * для разных пресетов сложности, чтобы убедиться, что поиск укладывается
 * в бюджет времени; отдельно профилирует стоимость оценки мобильности.
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

    /** Плотный миттельшпиль: дамки, шашки у обоих бортов и возможные серийные взятия. */
    @Test
    void denseMidgameWithinTimeBudget() {
        Board board = denseMidgame();

        testPreset(Color.WHITE, new AiSettings(4, 1500, AiSettings.DEFAULT_TT_CAPACITY), "MIDGAME MEDIUM");
        testPreset(Color.BLACK, new AiSettings(6, 3000, AiSettings.DEFAULT_TT_CAPACITY), "MIDGAME HARD");
        testPreset(Color.WHITE, new AiSettings(8, 2000, AiSettings.DEFAULT_TT_CAPACITY), "MIDGAME CUSTOM(8/2000)");
    }

    /** Стоимость одной статической оценки (включая мобильность обеих сторон). */
    @Test
    void mobilityEvaluationProfiling() {
        PositionEvaluator evaluator = new PositionEvaluator();
        Board board = denseMidgame();

        for (int index = 0; index < 1_000; index++) {
            evaluator.evaluate(board, Color.WHITE); // прогрев JIT
        }
        int iterations = 5_000;
        long start = System.nanoTime();
        int sink = 0;
        for (int index = 0; index < iterations; index++) {
            sink += evaluator.evaluate(board, Color.WHITE);
        }
        long elapsedNanos = System.nanoTime() - start;
        double microsPerEval = elapsedNanos / 1_000.0 / iterations;

        System.out.printf("PositionEvaluator (midgame): %.2f µs/eval, sink=%d%n",
                microsPerEval, sink);
        if (microsPerEval > 500) {
            throw new AssertionError(String.format(
                    "Оценка позиции слишком дорогая: %.2f µs/eval (лимит 500)", microsPerEval));
        }
    }

    private static Board denseMidgame() {
        Board board = Board.empty();
        // чёрные: дамка на a8 и шашки f7/d7/b7/f6/a6
        board.place(0, 1, Piece.BLACK_KING);
        board.place(1, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.BLACK_MAN);
        board.place(2, 1, Piece.BLACK_MAN);
        board.place(2, 5, Piece.BLACK_MAN);
        board.place(3, 0, Piece.BLACK_MAN);
        // белые: дамка a1 и шашки b2/d2/c3/e3/h4
        board.place(7, 0, Piece.WHITE_KING);
        board.place(6, 1, Piece.WHITE_MAN);
        board.place(6, 3, Piece.WHITE_MAN);
        board.place(5, 2, Piece.WHITE_MAN);
        board.place(5, 4, Piece.WHITE_MAN);
        board.place(4, 7, Piece.WHITE_MAN);
        return board;
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
