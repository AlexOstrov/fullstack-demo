package com.example.backend.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PieceTest {

    @Test
    void emptyCellHasNoColor() {
        assertTrue(Piece.EMPTY.isEmpty());
        assertThrows(IllegalStateException.class, Piece.EMPTY::color);
    }

    @Test
    void colorOfPieces() {
        assertEquals(Color.WHITE, Piece.WHITE_MAN.color());
        assertEquals(Color.WHITE, Piece.WHITE_KING.color());
        assertEquals(Color.BLACK, Piece.BLACK_MAN.color());
        assertEquals(Color.BLACK, Piece.BLACK_KING.color());
    }

    @Test
    void kingAndManFlags() {
        assertTrue(Piece.WHITE_KING.isKing());
        assertTrue(Piece.BLACK_KING.isKing());
        assertFalse(Piece.WHITE_MAN.isKing());
        assertTrue(Piece.WHITE_MAN.isMan());
        assertFalse(Piece.EMPTY.isMan());
        assertFalse(Piece.EMPTY.is(Color.WHITE));
    }

    @Test
    void factoryHelpers() {
        assertEquals(Piece.WHITE_MAN, Piece.man(Color.WHITE));
        assertEquals(Piece.BLACK_MAN, Piece.man(Color.BLACK));
        assertEquals(Piece.WHITE_KING, Piece.king(Color.WHITE));
        assertEquals(Piece.BLACK_KING, Piece.king(Color.BLACK));
    }

    @Test
    void colorOpposite() {
        assertEquals(Color.BLACK, Color.WHITE.opposite());
        assertEquals(Color.WHITE, Color.BLACK.opposite());
    }
}
