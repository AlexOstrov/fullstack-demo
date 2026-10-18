package com.example.backend.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Причина завершения партии (ТЗ 4.7): движок проставляет {@code endReason}
 * в момент перехода статуса в терминальный — источник колонки {@code end_reason}.
 */
class GameEndReasonTest {

    @Test
    void inProgressHasNoEndReason() {
        assertNull(GameState.newGame(Color.WHITE).endReason());
    }

    @Test
    void noMovesForOpponentGivesNoMovesReason() {
        // Белая шашка (1,0) идёт на (0,1) и превращается; у чёрных фигур нет — ходов нет.
        Board board = Board.empty();
        board.place(1, 0, Piece.WHITE_MAN);
        GameState game = GameState.ofPosition(board, Color.BLACK, Color.WHITE);

        game.applyMove(List.of(new Cell(1, 0), new Cell(0, 1)));

        assertEquals(GameStatus.WHITE_WON, game.status());
        assertEquals(GameEndReason.NO_MOVES, game.endReason());
    }

    @Test
    void resignGivesResignReason() {
        GameState game = GameState.newGame(Color.WHITE);

        game.resign(Color.WHITE);

        assertEquals(GameStatus.BLACK_WON, game.status());
        assertEquals(GameEndReason.RESIGN, game.endReason());
    }

    @Test
    void drawByAgreementGivesAgreementReason() {
        GameState game = GameState.newGame(Color.WHITE);

        game.drawByAgreement();

        assertEquals(GameStatus.DRAW, game.status());
        assertEquals(GameEndReason.DRAW_AGREEMENT, game.endReason());
    }

    @Test
    void threefoldRepetitionGivesRepetitionReason() {
        // Две дамки на разных диагоналях «туда-обратно»: 8 полуходов возвращают позицию в 3-й раз (ТЗ 3.6).
        Board board = Board.empty();
        board.place(4, 4, Piece.WHITE_KING);
        board.place(0, 1, Piece.BLACK_KING);
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);

        for (int cycle = 0; cycle < 2; cycle++) {
            game.applyMove(List.of(new Cell(4, 4), new Cell(3, 3)));
            game.applyMove(List.of(new Cell(0, 1), new Cell(1, 2)));
            game.applyMove(List.of(new Cell(3, 3), new Cell(4, 4)));
            game.applyMove(List.of(new Cell(1, 2), new Cell(0, 1)));
        }

        assertEquals(GameStatus.DRAW, game.status());
        assertEquals(GameEndReason.DRAW_REPETITION, game.endReason());
    }
}
