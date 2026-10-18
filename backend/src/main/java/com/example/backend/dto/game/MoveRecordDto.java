package com.example.backend.dto.game;

import com.example.backend.game.Move;
import com.example.backend.game.MoveType;

import java.util.List;

/** Запись истории ходов (ТЗ 5, {@code GET /api/game/{id}/moves}). */
public record MoveRecordDto(long moveNumber, PlayerColor player, MoveType type, List<CellDto> path) {

    public static MoveRecordDto from(long moveNumber, PlayerColor player, Move move) {
        return new MoveRecordDto(moveNumber, player, move.type(),
                move.path().stream().map(CellDto::of).toList());
    }
}
