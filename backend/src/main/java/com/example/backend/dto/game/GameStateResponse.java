package com.example.backend.dto.game;

import com.example.backend.game.GameEndReason;
import com.example.backend.game.GameStatus;

import java.util.List;
import java.util.UUID;

/**
 * Состояние партии в ответах API (ТЗ 5.2).
 * {@code board} — компактные токены: {@code ""}, {@code "w"}, {@code "W"}, {@code "b"}, {@code "B"}.
 * {@code ai} присутствует только в ответе {@code ai-move}.
 */
public record GameStateResponse(
        UUID gameId,
        PlayerColor humanColor,
        PlayerColor aiColor,
        GameStatus status,
        PlayerColor currentPlayer,
        long moveNumber,
        long version,
        List<List<String>> board,
        MoveDto lastMove,
        List<LegalMoveDto> legalMoves,
        boolean forcedCapture,
        GameEndReason endReason,
        AiTurnDto ai
) {
}
