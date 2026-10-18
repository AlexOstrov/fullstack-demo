package com.example.backend.dto.game;

import java.util.List;

/** История ходов и комментариев (ТЗ 5, {@code GET /api/game/{id}/moves}). */
public record MovesResponse(List<MoveRecordDto> moves, List<CommentDto> comments) {
}
