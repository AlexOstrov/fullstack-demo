package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Color;
import com.example.backend.game.Piece;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ZobristTest {

    private final Zobrist zobrist = new Zobrist();

    @Test
    void samePositionAndSideProduceSameHash() {
        Board board = Board.initialPosition();

        assertEquals(zobrist.hash(board, Color.WHITE), zobrist.hash(board.copy(), Color.WHITE),
                "копия той же позиции и очереди хода хэшируется одинаково");
    }

    @Test
    void sideToMoveChangesHash() {
        Board board = Board.initialPosition();

        assertNotEquals(zobrist.hash(board, Color.WHITE), zobrist.hash(board, Color.BLACK),
                "очередь хода — часть позиции (ТЗ 3.6: повторение считает и сторону)");
    }

    @Test
    void differentPositionsProduceDifferentHashes() {
        Board first = Board.initialPosition();
        Board second = Board.initialPosition();
        second.remove(2, 1);
        second.place(3, 2, Piece.BLACK_MAN);

        assertNotEquals(zobrist.hash(first, Color.WHITE), zobrist.hash(second, Color.WHITE));
    }

    @Test
    void pieceTypeChangesHash() {
        Board man = Board.empty();
        man.place(3, 2, Piece.WHITE_MAN);
        Board king = Board.empty();
        king.place(3, 2, Piece.WHITE_KING);

        assertNotEquals(zobrist.hash(man, Color.WHITE), zobrist.hash(king, Color.WHITE),
                "шашка и дамка на одной клетке — разные позиции");
    }

    @Test
    void keysAreDeterministic() {
        Board board = Board.initialPosition();

        assertEquals(new Zobrist().hash(board, Color.WHITE), zobrist.hash(board, Color.WHITE),
                "фиксированный seed — хэши воспроизводимы между запусками");
    }
}
