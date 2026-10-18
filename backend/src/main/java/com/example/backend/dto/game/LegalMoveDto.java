package com.example.backend.dto.game;

import java.util.List;

/** Допустимый ход для UI (ТЗ 5.2, {@code legalMoves}). */
public record LegalMoveDto(List<CellDto> path) {

    public static LegalMoveDto from(com.example.backend.game.Move move) {
        return new LegalMoveDto(move.path().stream().map(CellDto::of).toList());
    }
}
