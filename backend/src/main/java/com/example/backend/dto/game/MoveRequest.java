package com.example.backend.dto.game;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * Ход игрока (ТЗ 4.3, 5.1): полный путь и ожидаемый номер хода
 * для идемпотентности (ТЗ 5.4).
 */
public record MoveRequest(
        @NotEmpty List<@Valid CellDto> path,
        @PositiveOrZero int clientMoveNumber
) {
}
