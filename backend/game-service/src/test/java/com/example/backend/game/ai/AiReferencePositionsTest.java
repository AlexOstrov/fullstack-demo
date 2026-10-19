package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Cell;
import com.example.backend.game.Color;
import com.example.backend.game.Move;
import com.example.backend.game.MoveGenerator;
import com.example.backend.game.MoveType;
import com.example.backend.game.Piece;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Эталонные позиции (ТЗ 12: «Минимакс: выбор лучшего хода на заранее
 * известных позициях») и бенчмарк укладки в бюджет времени (DoD Sprint 2:
 * «укладывается в AI_TIME_BUDGET_MS»).
 */
class AiReferencePositionsTest {

    private final AlphaBetaSearch search = new AlphaBetaSearch();

    /** Эталонная позиция: максимальный материальный выигрыш — двойной бой с превращением. */
    @Test
    void prefersDoubleCaptureSeries() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(6, 3, Piece.WHITE_MAN);
        board.place(3, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.BLACK_MAN);
        board.place(0, 7, Piece.BLACK_MAN);

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.EASY));

        assertEquals(List.of(new Cell(4, 1), new Cell(2, 3), new Cell(0, 5)), result.move().path(),
                "серия снимает две шашки и превращается — максимум материала (ТЗ 3.4–3.5)");
        assertEquals(MoveType.PROMOTION, result.move().type(),
                "приземление на последнюю горизонталь — превращение в дамку");
    }

    /** Дамка не вешается: приземление под бой (1,6) отвергается в пользу безопасного. */
    @Test
    void doesNotHangKingToRecapture() {
        Board board = Board.empty();
        board.place(5, 2, Piece.WHITE_KING);
        board.place(6, 1, Piece.WHITE_MAN);
        board.place(3, 4, Piece.BLACK_MAN);
        board.place(0, 5, Piece.BLACK_MAN);

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.EASY));

        Move move = result.move();
        assertEquals(PositionEvaluator.MAN_VALUE, MoveOrdering.victimValue(board, move),
                "ИИ берёт шашку (3,4) — материально лучше тихого хода (+300 против +200)");
        Cell landing = move.path().get(move.path().size() - 1);
        assertFalse(landing.row() == 1 && landing.col() == 6,
                "приземление (1,6) отдаёт дамку чёрной шашке (0,5) — линия с оценкой 0");
    }

    /** Превращение: единственные разумные ходы — на последнюю горизонталь. */
    @Test
    void promotesWhenAdvantageous() {
        Board board = Board.empty();
        board.place(1, 2, Piece.WHITE_MAN);
        board.place(0, 7, Piece.BLACK_MAN);

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.EASY));

        assertEquals(MoveType.PROMOTION, result.move().type(),
                "шашка у края превращается в дамку (ТЗ 3.5)");
    }

    /** Бенчмарк: средний пресет укладывается в бюджет 1500 мс (ТЗ 6). */
    @Test
    void benchmarkStaysWithinMediumBudget() {
        Board board = Board.initialPosition();

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.MEDIUM));

        assertNotNull(result.move());
        assertTrue(MoveGenerator.generateMoves(board, Color.WHITE).contains(result.move()));
        assertTrue(result.elapsedMs() <= AiPreset.MEDIUM.timeBudgetMs() + 500,
                "MEDIUM: бюджет " + AiPreset.MEDIUM.timeBudgetMs() + " мс, фактически "
                        + result.elapsedMs() + " мс (" + result.nodes() + " узлов, глубина "
                        + result.depth() + ")");
    }

    /** Бенчмарк: сложный пресет укладывается в бюджет 3000 мс (ТЗ 6). */
    @Test
    void benchmarkStaysWithinHardBudget() {
        Board board = Board.initialPosition();

        AiResult result = search.findBestMove(board, Color.BLACK, AiSettings.of(AiPreset.HARD));

        assertNotNull(result.move());
        assertTrue(MoveGenerator.generateMoves(board, Color.BLACK).contains(result.move()));
        assertTrue(result.elapsedMs() <= AiPreset.HARD.timeBudgetMs() + 500,
                "HARD: бюджет " + AiPreset.HARD.timeBudgetMs() + " мс, фактически "
                        + result.elapsedMs() + " мс (" + result.nodes() + " узлов, глубина "
                        + result.depth() + ")");
    }
}
