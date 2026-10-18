package com.example.backend.game;

/**
 * Содержимое клетки доски: EMPTY, WHITE_MAN, BLACK_MAN, WHITE_KING, BLACK_KING (ТЗ 4.1).
 */
public enum Piece {
    EMPTY(null, false),
    WHITE_MAN(Color.WHITE, false),
    BLACK_MAN(Color.BLACK, false),
    WHITE_KING(Color.WHITE, true),
    BLACK_KING(Color.BLACK, true);

    private final Color color;
    private final boolean king;

    Piece(Color color, boolean king) {
        this.color = color;
        this.king = king;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    /** Цвет фигуры; для {@link #EMPTY} — ошибка. */
    public Color color() {
        if (color == null) {
            throw new IllegalStateException("У пустой клетки нет цвета");
        }
        return color;
    }

    public boolean isKing() {
        return king;
    }

    public boolean isMan() {
        return !isEmpty() && !king;
    }

    public boolean is(Color color) {
        return !isEmpty() && this.color == color;
    }

    public static Piece man(Color color) {
        return color == Color.WHITE ? WHITE_MAN : BLACK_MAN;
    }

    public static Piece king(Color color) {
        return color == Color.WHITE ? WHITE_KING : BLACK_KING;
    }
}
