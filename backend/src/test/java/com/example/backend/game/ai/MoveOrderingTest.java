package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Cell;
import com.example.backend.game.Move;
import com.example.backend.game.MoveType;
import com.example.backend.game.Piece;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveOrderingTest {

    private static Move move(MoveType type, int... rowColPairs) {
        java.util.ArrayList<Cell> path = new java.util.ArrayList<>();
        for (int i = 0; i < rowColPairs.length; i += 2) {
            path.add(new Cell(rowColPairs[i], rowColPairs[i + 1]));
        }
        return new Move(path, type);
    }

    @Test
    void capturesGoFirstByVictimValue() {
        Board board = Board.empty();
        board.place(3, 2, Piece.WHITE_MAN);
        board.place(2, 3, Piece.BLACK_MAN);
        board.place(4, 3, Piece.BLACK_KING);

        Move captureMan = move(MoveType.CAPTURE, 3, 2, 1, 4);
        Move captureKing = move(MoveType.CAPTURE, 3, 2, 5, 4);
        Move quiet = move(MoveType.NORMAL, 3, 2, 2, 1);

        List<Move> ordered = MoveOrdering.order(board, List.of(captureMan, quiet, captureKing),
                null, null, null);

        assertEquals(List.of(captureKing, captureMan, quiet), ordered,
                "бой дамки (+300) раньше боя шашки (+100), тихий — последним (ТЗ 6)");
    }

    @Test
    void ttMoveLeadsItsGroup() {
        Board board = Board.empty();
        board.place(3, 2, Piece.WHITE_MAN);

        Move quietA = move(MoveType.NORMAL, 3, 2, 2, 1);
        Move quietB = move(MoveType.NORMAL, 3, 2, 2, 3);

        List<Move> ordered = MoveOrdering.order(board, List.of(quietB, quietA),
                quietA, null, null);

        assertEquals(List.of(quietA, quietB), ordered, "TT-ход — впереди тихих");
    }

    @Test
    void killersComeAfterTtAndBeforeOtherQuiets() {
        Board board = Board.empty();
        board.place(3, 2, Piece.WHITE_MAN);

        Move quietA = move(MoveType.NORMAL, 3, 2, 2, 1);
        Move quietB = move(MoveType.NORMAL, 3, 2, 2, 3);
        Move killer = move(MoveType.NORMAL, 3, 2, 1, 0);

        List<Move> withTt = MoveOrdering.order(board, List.of(quietA, quietB, killer),
                quietA, killer, null);
        assertEquals(List.of(quietA, killer, quietB), withTt,
                "порядок: TT-ход, «убийственные», прочие (ТЗ 6)");

        List<Move> withoutTt = MoveOrdering.order(board, List.of(quietA, quietB, killer),
                null, killer, null);
        assertEquals(List.of(killer, quietA, quietB), withoutTt);
    }

    @Test
    void captureDetectionIncludesPromotedSingleJump() {
        Board board = Board.empty();
        board.place(2, 3, Piece.WHITE_MAN);
        board.place(1, 4, Piece.BLACK_MAN);

        Move promotedJump = move(MoveType.PROMOTION, 2, 3, 0, 5);
        assertTrue(MoveOrdering.isCapture(board, promotedJump),
                "одиночный прыжок с превращением (ТЗ 3.5) — взятие, а не тихий ход");
        assertEquals(PositionEvaluator.MAN_VALUE, MoveOrdering.victimValue(board, promotedJump));

        Board quietPromotionBoard = Board.empty();
        quietPromotionBoard.place(1, 2, Piece.WHITE_MAN);
        Move quietPromotion = move(MoveType.PROMOTION, 1, 2, 0, 1);
        assertFalse(MoveOrdering.isCapture(quietPromotionBoard, quietPromotion),
                "тихое превращение (без жертвы) — не взятие");

        Board seriesBoard = Board.empty();
        seriesBoard.place(4, 1, Piece.WHITE_MAN);
        seriesBoard.place(3, 2, Piece.BLACK_MAN);
        seriesBoard.place(1, 4, Piece.BLACK_MAN);
        Move series = move(MoveType.CAPTURE, 4, 1, 2, 3, 0, 5);
        assertEquals(2 * PositionEvaluator.MAN_VALUE, MoveOrdering.victimValue(seriesBoard, series),
                "серия из двух боёв — сумма жертв");
    }

    @Test
    void kingVictimCostsThreeHundred() {
        Board board = Board.empty();
        board.place(2, 1, Piece.WHITE_MAN);
        board.place(1, 2, Piece.BLACK_KING);

        Move captureKing = move(MoveType.CAPTURE, 2, 1, 0, 3);
        assertEquals(PositionEvaluator.KING_VALUE, MoveOrdering.victimValue(board, captureKing));
    }
}
