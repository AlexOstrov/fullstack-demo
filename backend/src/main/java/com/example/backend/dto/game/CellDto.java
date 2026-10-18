package com.example.backend.dto.game;

import com.example.backend.game.Cell;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Клетка пути хода (ТЗ 4.3, 5.1): row/col 0..7.
 * Отдельный DTO, чтобы валидировать вход и не смешивать с доменной {@link Cell}.
 */
public record CellDto(@Min(0) @Max(7) int row, @Min(0) @Max(7) int col) {

    public static CellDto of(Cell cell) {
        return new CellDto(cell.row(), cell.col());
    }

    public Cell toCell() {
        return new Cell(row, col);
    }
}
