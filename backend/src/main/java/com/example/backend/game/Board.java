package com.example.backend.game;

import java.util.Arrays;
import java.util.Objects;

/**
 * Доска 8×8 (ТЗ 3.1, 4.1).
 * Игровые клетки — тёмные, где (row + col) нечётно.
 * Координаты фиксированы независимо от цвета игрока.
 */
public final class Board {

    public static final int SIZE = 8;

    private final Piece[][] cells;

    private Board() {
        this.cells = new Piece[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            Arrays.fill(cells[row], Piece.EMPTY);
        }
    }

    /** Пустая доска. */
    public static Board empty() {
        return new Board();
    }

    /**
     * Начальная расстановка (ТЗ 3.1): белые — тёмные клетки рядов 5–7 (12 шашек),
     * чёрные — рядов 0–2 (12 шашек).
     */
    public static Board initialPosition() {
        Board board = new Board();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (isDark(row, col)) {
                    board.cells[row][col] = Piece.BLACK_MAN;
                }
            }
        }
        for (int row = 5; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (isDark(row, col)) {
                    board.cells[row][col] = Piece.WHITE_MAN;
                }
            }
        }
        return board;
    }

    /** Тёмная (игровая) клетка. */
    public static boolean isDark(int row, int col) {
        return (row + col) % 2 != 0;
    }

    public static boolean inBounds(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public Piece at(int row, int col) {
        checkBounds(row, col);
        return cells[row][col];
    }

    public void place(int row, int col, Piece piece) {
        checkBounds(row, col);
        cells[row][col] = Objects.requireNonNull(piece, "piece");
    }

    public void remove(int row, int col) {
        place(row, col, Piece.EMPTY);
    }

    /** Число фигур (шашек и дамок) данного цвета — для счёта. */
    public int count(Color color) {
        int total = 0;
        for (Piece[] row : cells) {
            for (Piece piece : row) {
                if (piece.is(color)) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Ключ расстановки для таблицы повторений: 64 символа
     * ('.' пусто, 'w'/'W' белые, 'b'/'B' чёрные).
     */
    public String positionKey() {
        StringBuilder key = new StringBuilder(SIZE * SIZE);
        for (Piece[] row : cells) {
            for (Piece piece : row) {
                key.append(symbol(piece));
            }
        }
        return key.toString();
    }

    /** Независимая копия поля. */
    public Piece[][] snapshot() {
        Piece[][] copy = new Piece[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            copy[row] = Arrays.copyOf(cells[row], SIZE);
        }
        return copy;
    }

    private static char symbol(Piece piece) {
        return switch (piece) {
            case EMPTY -> '.';
            case WHITE_MAN -> 'w';
            case WHITE_KING -> 'W';
            case BLACK_MAN -> 'b';
            case BLACK_KING -> 'B';
        };
    }

    private static void checkBounds(int row, int col) {
        if (!inBounds(row, col)) {
            throw new IndexOutOfBoundsException(
                    "Клетка вне доски: row=" + row + ", col=" + col);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Board that)) {
            return false;
        }
        return Arrays.deepEquals(cells, that.cells);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(cells);
    }
}
