package com.example.backend.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveGeneratorTest {

    // --- Простые ходы ---

    @Test
    void initialPositionHasFourWhiteForwardMoves() {
        Board board = Board.initialPosition();

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        assertEquals(7, moves.size(),
                "с начальной позиции у белых7 ходов — все из ряда5 в ряд4, у (5,0) лишь одна диагональ (ТЗ 3.1)");
        assertFalse(MoveGenerator.hasCaptures(board, Color.WHITE));
        for (Move move : moves) {
            assertEquals(MoveType.NORMAL, move.type());
            assertEquals(2, move.path().size());
            Cell from = move.path().get(0);
            Cell to = move.path().get(1);
            assertEquals(1, from.row() - to.row(), "белые ходят вверх (уменьшение row)");
        }
    }

    @Test
    void simpleMovesOnlyForward() {
        Board whiteBoard = Board.empty();
        whiteBoard.place(3, 2, Piece.WHITE_MAN);
        List<Move> whiteMoves = MoveGenerator.generateMoves(whiteBoard, Color.WHITE);
        assertEquals(2, whiteMoves.size());
        assertTrue(whiteMoves.stream().allMatch(m -> m.path().get(1).row() == 2),
                "белая шашка ходит только вверх");

        Board blackBoard = Board.empty();
        blackBoard.place(3, 2, Piece.BLACK_MAN);
        List<Move> blackMoves = MoveGenerator.generateMoves(blackBoard, Color.BLACK);
        assertEquals(2, blackMoves.size());
        assertTrue(blackMoves.stream().allMatch(m -> m.path().get(1).row() == 4),
                "чёрная шашка ходит только вниз");
    }

    @Test
    void manCapturesBackward() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN);

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        assertEquals(1, moves.size());
        Move move = moves.get(0);
        assertEquals(MoveType.CAPTURE, move.type());
        assertEquals(new Cell(4, 1), move.path().get(0));
        assertEquals(new Cell(6, 3), move.path().get(1), "простая шашка бьёт и назад (ТЗ 3.2)");
    }

    @Test
    void capturesAreMandatory() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN); // взятие для (4,1)
        board.place(6, 5, Piece.WHITE_MAN); // у этой шашки есть простые ходы

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        assertEquals(1, moves.size(), "при наличии взятия простые ходы запрещены (ТЗ 3.4)");
        assertEquals(MoveType.CAPTURE, moves.get(0).type());
    }

    // --- Серийные взятия ---

    @Test
    void seriesOfTwoJumpsIsOneMove() {
        Board board = Board.empty();
        board.place(6, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN);
        board.place(3, 4, Piece.BLACK_MAN);

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        assertEquals(1, moves.size());
        Move move = moves.get(0);
        assertEquals(MoveType.CAPTURE, move.type());
        assertEquals(List.of(new Cell(6, 1), new Cell(4, 3), new Cell(2, 5)), move.path());
    }

    @Test
    void seriesRunsToTheEndNoDoubleHit() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(3, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.BLACK_MAN);

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        // взятая шашка снята сразу → серия идёт до упора, коротких вариантов нет
        assertEquals(1, moves.size());
        assertEquals(List.of(new Cell(4, 1), new Cell(2, 3), new Cell(0, 5)), moves.get(0).path());
        assertEquals(MoveType.PROMOTION, moves.get(0).type(), "приземление на row 0 — превращение (ТЗ 3.5)");
    }

    @Test
    void promotionContinuesAsKingImmediately() {
        Board board = Board.empty();
        board.place(2, 1, Piece.WHITE_MAN);
        board.place(1, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.BLACK_MAN);

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        assertEquals(3, moves.size(), "после превращения дамка бьёт с приземлениями (2,5), (3,6), (4,7)");
        assertTrue(moves.stream().allMatch(m -> m.type() == MoveType.PROMOTION));
        // приземление «через клетку» за жертвой возможно только дамкой (ТЗ 3.3, 3.5)
        assertTrue(moves.stream().anyMatch(m -> m.path().get(m.path().size() - 1).equals(new Cell(4, 7))));
    }

    // --- Дамки ---

    @Test
    void kingSimpleMovesAnyDistance() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_KING);

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        assertEquals(9, moves.size(), "дамка ходит на любое число свободных клеток (ТЗ 3.3)");
        assertTrue(moves.stream().allMatch(m -> m.type() == MoveType.NORMAL));
        assertTrue(moves.stream().allMatch(m -> m.path().size() == 2));
    }

    @Test
    void kingCaptureOffersAllLandingCells() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_KING);
        board.place(3, 2, Piece.BLACK_MAN);

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        assertEquals(3, moves.size(), "приземления за жертвой: (2,3), (1,4), (0,5)");
        assertTrue(moves.stream().allMatch(m -> m.type() == MoveType.CAPTURE));
        assertTrue(moves.stream().allMatch(m -> m.path().get(1).equals(new Cell(2, 3))
                || m.path().get(1).equals(new Cell(1, 4))
                || m.path().get(1).equals(new Cell(0, 5))));
    }

    @Test
    void kingLandingStopsBeforeOccupiedCell() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_KING);
        board.place(3, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.WHITE_MAN); // блокирует приземления (1,4) и дальше

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        assertEquals(1, moves.size(), "приземления только до следующей занятой клетки (ТЗ 3.3)");
        assertEquals(new Cell(2, 3), moves.get(0).path().get(1));
    }

    @Test
    void kingCannotJumpOverTwoAdjacentEnemies() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_KING);
        board.place(3, 2, Piece.BLACK_MAN);
        board.place(2, 3, Piece.BLACK_MAN); // за жертвой сразу занята — приземлений нет

        List<Move> moves = MoveGenerator.generateMoves(board, Color.WHITE);

        assertFalse(MoveGenerator.hasCaptures(board, Color.WHITE), "дамка бьёт ровно через одну (ТЗ 3.3)");
        assertEquals(5, moves.size(), "остаются только простые ходы");
        assertTrue(moves.stream().allMatch(m -> m.type() == MoveType.NORMAL));
    }

    // --- Вырожденные случаи ---

    @Test
    void noMovesOnEmptyOrForeignBoard() {
        assertTrue(MoveGenerator.generateMoves(Board.empty(), Color.WHITE).isEmpty());

        Board whiteOnly = Board.empty();
        whiteOnly.place(3, 2, Piece.WHITE_MAN);
        assertTrue(MoveGenerator.generateMoves(whiteOnly, Color.BLACK).isEmpty());
    }
}
