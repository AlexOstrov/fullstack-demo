package com.example.backend.game;

/** Статус партии (ТЗ 4.1). */
public enum GameStatus {
    IN_PROGRESS,
    WHITE_WON,
    BLACK_WON,
    DRAW;

    /** Терминальный статус: партия завершена (ТЗ 3.6). */
    public boolean isTerminal() {
        return this != IN_PROGRESS;
    }

    /** Победивший цвет или {@code null} при ничьей/незавершённой партии. */
    public Color winnerColor() {
        return switch (this) {
            case WHITE_WON -> Color.WHITE;
            case BLACK_WON -> Color.BLACK;
            default -> null;
        };
    }
}
