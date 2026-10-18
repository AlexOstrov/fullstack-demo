package com.example.backend.game;

/** Цвет игрока/шашки. Белые ходят первыми (ТЗ 3.1). */
public enum Color {
    WHITE,
    BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** Последняя горизонталь соперника, на которой шашка превращается в дамку (ТЗ 3.5). */
    public boolean isPromotionRow(int row) {
        return this == WHITE ? row == 0 : row == Board.SIZE - 1;
    }
}
