package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Cell;
import com.example.backend.game.Color;
import com.example.backend.game.MoveGenerator;
import com.example.backend.game.Piece;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaBetaSearchTest {

    private final AlphaBetaSearch search = new AlphaBetaSearch();

    @Test
    void returnsLegalMoveFromInitialPosition() {
        Board board = Board.initialPosition();

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.MEDIUM));

        assertNotNull(result.move());
        assertTrue(MoveGenerator.generateMoves(board, Color.WHITE).contains(result.move()),
                "ход ИИ обязан быть из списка допустимых ходов");
        assertTrue(result.depth() >= 1, "итеративное углубление завершает хотя бы первую итерацию");
        assertTrue(result.nodes() > 0);
    }

    @Test
    void capturesAreForcedSoQuietMovesAreIgnored() {
        Board board = Board.empty();
        board.place(2, 1, Piece.WHITE_MAN);
        board.place(6, 5, Piece.WHITE_MAN);
        board.place(1, 2, Piece.BLACK_MAN);

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.EASY));

        assertEquals(List.of(new Cell(2, 1), new Cell(0, 3)), result.move().path(),
                "при наличии боя генератор (и ИИ) предлагает только взятия (ТЗ 3.4)");
    }

    @Test
    void findsWinBySeriesCapturingAllOpponentPieces() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(6, 5, Piece.WHITE_MAN);
        board.place(3, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.BLACK_MAN);

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.EASY));

        assertEquals(List.of(new Cell(4, 1), new Cell(2, 3), new Cell(0, 5)), result.move().path(),
                "серия снимает обе чёрные; чёрные без ходов — конец партии (ТЗ 3.6)");
        assertTrue(result.score() >= AlphaBetaSearch.MATE_SCORE - 16,
                "выигранная партия должна оцениваться как «мат»: " + result.score());
    }

    @Test
    void prefersCapturingKingOverMan() {
        Board board = Board.empty();
        board.place(3, 2, Piece.WHITE_MAN);
        board.place(2, 3, Piece.BLACK_MAN);
        board.place(4, 3, Piece.BLACK_KING);

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.EASY));

        assertEquals(List.of(new Cell(3, 2), new Cell(5, 4)), result.move().path(),
                "бой дамки (+300) материальнее боя шашки (+100)");
    }

    @Test
    void noLegalMoveReturnsNullWithLossScore() {
        Board board = Board.empty();
        board.place(3, 2, Piece.WHITE_MAN);

        AiResult result = search.findBestMove(board, Color.BLACK, AiSettings.of(AiPreset.EASY));

        assertNull(result.move(), "у чёрных нет ни фигур, ни ходов");
        assertEquals(-AlphaBetaSearch.MATE_SCORE, result.score(),
                "нет ходов — поражение стороны, чей ход (ТЗ 3.6)");
    }

    @Test
    void smallTranspositionTableDoesNotBreakChoice() {
        Board board = Board.empty();
        board.place(3, 2, Piece.WHITE_MAN);
        board.place(2, 3, Piece.BLACK_MAN);
        board.place(4, 3, Piece.BLACK_KING);
        AiSettings settings = new AiSettings(2, AiSettings.MIN_TIME_BUDGET_MS, 64);

        AiResult result = search.findBestMove(board, Color.WHITE, settings);

        assertEquals(List.of(new Cell(3, 2), new Cell(5, 4)), result.move().path(),
                "тактический выбор не зависит от размера TT");
    }

    @Test
    void staysWithinTimeBudget() {
        Board board = Board.initialPosition();
        AiSettings settings = new AiSettings(AiSettings.MAX_DEPTH, 500,
                AiSettings.DEFAULT_TT_CAPACITY);

        AiResult result = search.findBestMove(board, Color.WHITE, settings);

        assertTrue(result.elapsedMs() <= 900,
                "бюджет 500 мс + запас на шум ОС: " + result.elapsedMs() + " мс");
        assertNotNull(result.move());
        assertTrue(result.depth() >= 1);
    }

    @Test
    void minimalBudgetStillAnswers() {
        Board board = Board.initialPosition();
        AiSettings settings = new AiSettings(AiSettings.MAX_DEPTH, AiSettings.MIN_TIME_BUDGET_MS,
                AiSettings.DEFAULT_TT_CAPACITY);

        AiResult result = search.findBestMove(board, Color.BLACK, settings);

        assertNotNull(result.move(), "итеративное углубление гарантирует ответ при любом бюджете");
        assertTrue(result.elapsedMs() <= 600,
                "минимальный бюджет 200 мс + запас: " + result.elapsedMs() + " мс");
    }
}
