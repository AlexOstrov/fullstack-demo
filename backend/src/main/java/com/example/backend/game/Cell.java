package com.example.backend.game;

/**
 * Клетка доски: row 0..7 сверху вниз, col 0..7 слева направо (ТЗ 3.1).
 * Границы проверяет движок (валидация пути — ТЗ 4.4).
 */
public record Cell(int row, int col) {
}
