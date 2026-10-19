package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Cell;
import com.example.backend.game.Move;
import com.example.backend.game.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Сортировка ходов для альфа-беты (ТЗ 6): сначала взятия — и особенно
 * с максимальной ценой снятых шашек (бой дамки), затем TT-ход, затем
 * «убийственные» (киллер) тихие ходы, затем прочие.
 * <p>При наличии взятий все ходы узла — взятия с равным числом снятых
 * (правило большинства, ТЗ 3.4), поэтому цена снимаемого определяет порядок.
 */
public final class MoveOrdering {

    private MoveOrdering() {
    }

    /** Упорядоченная копия ходов. Killers — «убийственные» тихие ходы плоскости ply. */
    public static List<Move> order(Board board, List<Move> moves, Move ttMove,
                                   Move killerOne, Move killerTwo) {
        List<Move> captures = new ArrayList<>();
        List<Move> quiets = new ArrayList<>();
        for (Move move : moves) {
            if (isCapture(board, move)) {
                captures.add(move);
            } else {
                quiets.add(move);
            }
        }
        captures.sort((left, right) ->
                Integer.compare(victimValue(board, right), victimValue(board, left)));
        moveToFront(captures, ttMove); // TT-ход — в начало взятий

        moveToFront(quiets, killerOne); // «убийственные» тихие ходы
        moveToFront(quiets, killerTwo);
        moveToFront(quiets, ttMove);    // затем TT-ход — в начало тихих (если взятий нет)

        List<Move> ordered = new ArrayList<>(captures.size() + quiets.size());
        ordered.addAll(captures);
        ordered.addAll(quiets);
        return ordered;
    }

    /** Ход со снятием: хотя бы один сегмент пути имеет жертву между клетками. */
    public static boolean isCapture(Board board, Move move) {
        return victimValue(board, move) > 0;
    }

    /** Суммарная ценность снятых шашек (дамка 300, шашка 100 — ТЗ 6). */
    public static int victimValue(Board board, Move move) {
        List<Cell> path = move.path();
        int total = 0;
        for (int i = 1; i < path.size(); i++) {
            Piece victim = findFirstBetween(board, path.get(i - 1), path.get(i));
            if (victim != null && !victim.isEmpty()) {
                total += victim.isKing()
                        ? PositionEvaluator.KING_VALUE
                        : PositionEvaluator.MAN_VALUE;
            }
        }
        return total;
    }

    private static Piece findFirstBetween(Board board, Cell from, Cell to) {
        int rowStep = Integer.signum(to.row() - from.row());
        int colStep = Integer.signum(to.col() - from.col());
        int row = from.row() + rowStep;
        int col = from.col() + colStep;
        while (row != to.row() || col != to.col()) {
            Piece piece = board.at(row, col);
            if (!piece.isEmpty()) {
                return piece;
            }
            row += rowStep;
            col += colStep;
        }
        return null;
    }

    private static void moveToFront(List<Move> moves, Move move) {
        if (move != null && moves.remove(move)) {
            moves.add(0, move);
        }
    }
}
