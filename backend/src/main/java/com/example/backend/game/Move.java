package com.example.backend.game;

import java.util.List;

/**
 * Применённый ход: полный путь (см. ТЗ 4.3) и его тип.
 * Обычный ход — 2 точки, серийное взятие — 3 и более.
 */
public record Move(List<Cell> path, MoveType type) {

    public Move {
        path = List.copyOf(path);
    }
}
