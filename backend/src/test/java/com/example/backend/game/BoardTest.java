package com.example.backend.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

    @Test
    void emptyBoardHas64EmptyCells() {
        Board board = Board.empty();

        assertEquals(64, Board.SIZE * Board.SIZE);
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                assertTrue(board.at(row, col).isEmpty());
            }
        }
    }

    @Test
    void initialPositionHas12PiecesPerColor() {
        Board board = Board.initialPosition();

        assertEquals(12, board.count(Color.WHITE));
        assertEquals(12, board.count(Color.BLACK));
    }

    @Test
    void initialPositionUsesOnlyDarkCells() {
        Board board = Board.initialPosition();

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (!board.at(row, col).isEmpty()) {
                    assertTrue(Board.isDark(row, col),
                            "фигура на светлой клетке row=" + row + ", col=" + col);
                }
            }
        }
    }

    @Test
    void initialPositionRanksMatchSpec() {
        Board board = Board.initialPosition();

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                boolean dark = Board.isDark(row, col);
                if (row <= 2) {
                    assertEquals(dark ? Piece.BLACK_MAN : Piece.EMPTY, piece,
                            "ряды 0–2, col=" + col);
                } else if (row >= 5) {
                    assertEquals(dark ? Piece.WHITE_MAN : Piece.EMPTY, piece,
                            "ряды 5–7, col=" + col);
                } else {
                    assertTrue(piece.isEmpty(), "ряды 3–4 должны быть пустыми, row=" + row);
                }
            }
        }
    }

    @Test
    void initialPositionContainsOnlyMen() {
        Board board = Board.initialPosition();

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                if (!piece.isEmpty()) {
                    assertTrue(piece.isMan());
                    assertFalse(piece.isKing());
                }
            }
        }
    }

    @Test
    void atRejectsCoordinatesOutsideBoard() {
        Board board = Board.empty();

        assertThrows(IndexOutOfBoundsException.class, () -> board.at(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.at(0, 8));
        assertThrows(IndexOutOfBoundsException.class, () -> board.at(8, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.place(0, -1, Piece.WHITE_MAN));
    }

    @Test
    void placeAndRemoveUpdateCell() {
        Board board = Board.empty();

        board.place(0, 1, Piece.WHITE_KING);
        assertEquals(Piece.WHITE_KING, board.at(0, 1));

        board.remove(0, 1);
        assertTrue(board.at(0, 1).isEmpty());

        assertThrows(NullPointerException.class, () -> board.place(1, 2, null));
    }

    @Test
    void positionKeyIsStableAndDistinguishesPositions() {
        Board empty = Board.empty();
        Board initial1 = Board.initialPosition();
        Board initial2 = Board.initialPosition();

        assertEquals(64, empty.positionKey().length());
        assertEquals(".".repeat(64), empty.positionKey());
        assertEquals(initial1.positionKey(), initial2.positionKey());
        assertNotEquals(empty.positionKey(), initial1.positionKey());
        assertEquals(initial1, initial2);
        assertNotEquals(empty, initial1);
    }

    @Test
    void snapshotIsIndependentCopy() {
        Board board = Board.initialPosition();
        Piece[][] copy = board.snapshot();

        board.place(0, 1, Piece.EMPTY);
        assertEquals(Piece.BLACK_MAN, copy[0][1]);
    }

    @Test
    void isDarkMatchesSpec() {
        assertTrue(Board.isDark(0, 1));
        assertTrue(Board.isDark(1, 0));
        assertFalse(Board.isDark(0, 0));
        assertFalse(Board.isDark(1, 1));
        assertFalse(Board.inBounds(-1, 0));
        assertTrue(Board.inBounds(7, 7));
    }
}
