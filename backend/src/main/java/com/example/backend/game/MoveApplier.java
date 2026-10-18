package com.example.backend.game;

import java.util.List;

/**
 * Применение валидного хода к доске (ТЗ 4.5): снятие взятых шашек
 * по мере прыжка, немедленное превращение при приземлении на последнюю
 * горизонталь (ТЗ 3.5). Общая логика для движка партии и поиска ИИ.
 * <p>Путь должен быть предварительно проверен генератором/валидатором.
 */
public final class MoveApplier {

    private MoveApplier() {
    }

    /** Применяет ход к этой доске без копирования — для движка партии. */
    public static void applyInPlace(Board board, Move move) {
        List<Cell> path = move.path();
        Piece piece = board.at(path.get(0).row(), path.get(0).col());

        for (int i = 1; i < path.size(); i++) {
            Cell from = path.get(i - 1);
            Cell to = path.get(i);

            Cell victim = findVictimOrSlide(board, from, to);
            if (victim != null) {
                board.remove(victim.row(), victim.col());
            }

            board.remove(from.row(), from.col());

            if (piece.isMan() && piece.color().isPromotionRow(to.row())) {
                piece = Piece.king(piece.color()); // немедленно при приземлении (ТЗ 3.5)
            }
            board.place(to.row(), to.col(), piece);
        }
    }

    /** Независимая копия доски с применённым ходом — для поиска минимакса (ТЗ 6). */
    public static Board apply(Board board, Move move) {
        Board copy = board.copy();
        applyInPlace(copy, move);
        return copy;
    }

    /**
     * Жертва прыжка: первая непустая клетка строго между from и to
     * (для шашки — середина); null — если промежуток пуст (тихий ход).
     */
    private static Cell findVictimOrSlide(Board board, Cell from, Cell to) {
        int rowStep = Integer.signum(to.row() - from.row());
        int colStep = Integer.signum(to.col() - from.col());
        int row = from.row() + rowStep;
        int col = from.col() + colStep;
        while (row != to.row() || col != to.col()) {
            if (!board.at(row, col).isEmpty()) {
                return new Cell(row, col);
            }
            row += rowStep;
            col += colStep;
        }
        return null;
    }
}
