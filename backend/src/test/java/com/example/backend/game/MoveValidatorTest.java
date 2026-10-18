package com.example.backend.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveValidatorTest {

    // --- analyze: список допустимых ходов и forcedCapture (ТЗ 4.4) ---

    @Test
    void analyzeOnInitialPositionHasMovesWithoutCapture() {
        MoveValidator.Legal legal = MoveValidator.analyze(Board.initialPosition(), Color.WHITE);

        assertEquals(7, legal.moves().size());
        assertTrue(legal.captures().isEmpty());
        assertFalse(legal.forcedCapture(), "с начальной позиции взятий нет");
    }

    @Test
    void analyzeExposesForcedCaptures() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN);
        board.place(6, 5, Piece.WHITE_MAN); // у этой шашки есть простые ходы

        MoveValidator.Legal legal = MoveValidator.analyze(board, Color.WHITE);

        assertTrue(legal.forcedCapture(), "при наличии взятия оно обязательно (ТЗ 3.4)");
        assertEquals(legal.captures(), legal.moves(), "допустимы только взятия");
        assertEquals(1, legal.moves().size());
    }

    @Test
    void findMatchesPathAndRejectsUnknown() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN);

        MoveValidator.Legal legal = MoveValidator.analyze(board, Color.WHITE);

        assertEquals(MoveType.CAPTURE, legal.find(List.of(new Cell(4, 1), new Cell(6, 3))).type());
        assertNull(legal.find(List.of(new Cell(4, 1), new Cell(3, 2))), "простого хода нет во взятиях");
    }

    // --- validate: корректные ходы ---

    @Test
    void validateAcceptsSimpleMoveAndReturnsItsType() {
        Board board = Board.initialPosition();

        Move move = MoveValidator.validate(board, Color.WHITE,
                List.of(new Cell(5, 0), new Cell(4, 1)));

        assertEquals(MoveType.NORMAL, move.type());
        assertEquals(List.of(new Cell(5, 0), new Cell(4, 1)), move.path());
    }

    @Test
    void validateAcceptsCaptureSeriesAndReturnsItsType() {
        Board board = Board.empty();
        board.place(6, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN);
        board.place(3, 4, Piece.BLACK_MAN);

        Move move = MoveValidator.validate(board, Color.WHITE,
                List.of(new Cell(6, 1), new Cell(4, 3), new Cell(2, 5)));

        assertEquals(MoveType.CAPTURE, move.type());
    }

    @Test
    void validateAcceptsKingCaptureWithFarLanding() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_KING);
        board.place(3, 2, Piece.BLACK_MAN);

        Move move = MoveValidator.validate(board, Color.WHITE,
                List.of(new Cell(4, 1), new Cell(0, 5)));

        assertEquals(MoveType.CAPTURE, move.type(), "дамка приземляется на любую клетку за жертвой");
    }

    // --- validate: отказы по форме пути ---

    @Test
    void validateRejectsPathShorterThanTwoCells() {
        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(Board.initialPosition(), Color.WHITE,
                        List.of(new Cell(5, 0))));
        assertTrue(error.getMessage().contains("хотя бы две клетки"));
    }

    @Test
    void validateRejectsCellOutsideBoard() {
        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(Board.initialPosition(), Color.WHITE,
                        List.of(new Cell(5, 0), new Cell(4, 8))));
        assertTrue(error.getMessage().contains("вне доски"));
    }

    @Test
    void validateRejectsNonDiagonalStep() {
        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(Board.initialPosition(), Color.WHITE,
                        List.of(new Cell(5, 0), new Cell(4, 0))));
        assertTrue(error.getMessage().contains("не по диагонали"));
    }

    @Test
    void validateRejectsRepeatingSameCell() {
        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(Board.initialPosition(), Color.WHITE,
                        List.of(new Cell(5, 0), new Cell(5, 0))));
        assertTrue(error.getMessage().contains("не по диагонали"));
    }

    // --- validate: отказы по правилам ---

    @Test
    void validateRejectsEmptyStartCell() {
        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(Board.initialPosition(), Color.WHITE,
                        List.of(new Cell(3, 2), new Cell(4, 1))));
        assertTrue(error.getMessage().contains("нет шашки"));
    }

    @Test
    void validateRejectsEnemyPieceAtStart() {
        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(Board.initialPosition(), Color.WHITE,
                        List.of(new Cell(2, 1), new Cell(3, 0))));
        assertTrue(error.getMessage().contains("чужой шашке"));
    }

    @Test
    void validateRejectsBackwardManMove() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);

        assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(board, Color.WHITE,
                        List.of(new Cell(4, 1), new Cell(5, 2))),
                "простая шашка ходит только вперёд (ТЗ 3.2)");
    }

    @Test
    void validateRejectsMoveIgnoringForcedCapture() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN); // обязательное взятие
        board.place(6, 5, Piece.WHITE_MAN);

        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(board, Color.WHITE,
                        List.of(new Cell(6, 5), new Cell(5, 4))));
        assertTrue(error.getMessage().contains("обязательное взятие"));
    }

    @Test
    void validateRejectsShorterCaptureSeries() {
        Board board = Board.empty();
        board.place(6, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN);
        board.place(3, 4, Piece.BLACK_MAN);

        assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(board, Color.WHITE,
                        List.of(new Cell(6, 1), new Cell(4, 3))),
                "бой продолжается до упора (ТЗ 3.4)");
    }

    @Test
    void validateRejectsCaptureViolatingMajorityRule() {
        Board board = Board.empty();
        // серия на2: (6,5)→(4,7) через (5,6), затем (2,5) через (3,6)
        board.place(6, 5, Piece.WHITE_MAN);
        board.place(5, 6, Piece.BLACK_MAN);
        board.place(3, 6, Piece.BLACK_MAN);
        // взятие на1: (4,1)→(6,3) через (5,2)
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN);

        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(board, Color.WHITE,
                        List.of(new Cell(4, 1), new Cell(6, 3))));
        assertTrue(error.getMessage().contains("правило большинства"));
    }

    @Test
    void validateRejectsKingLandingBeyondOccupiedCell() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_KING);
        board.place(3, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.WHITE_MAN); // приземления только до (2,3)

        assertThrows(InvalidMoveException.class,
                () -> MoveValidator.validate(board, Color.WHITE,
                        List.of(new Cell(4, 1), new Cell(1, 4))),
                "дамка не приземляется за занятой клеткой (ТЗ 3.3)");
    }
}
