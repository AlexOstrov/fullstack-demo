package com.example.backend.game;

/** Цвет игрока/шашки. Белые ходят первыми (ТЗ 3.1). */
public enum Color {
    WHITE,
    BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
