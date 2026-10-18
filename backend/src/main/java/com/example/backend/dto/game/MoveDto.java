package com.example.backend.dto.game;

import com.example.backend.game.Move;
import com.example.backend.game.MoveType;

import java.util.List;

/** Ход в ответе API (ТЗ 5.2): путь и тип. */
public record MoveDto(List<CellDto> path, MoveType type) {

    public static MoveDto from(Move move) {
        return new MoveDto(move.path().stream().map(CellDto::of).toList(), move.type());
    }
}
