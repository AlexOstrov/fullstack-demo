package com.example.backend.dto.game;

/**
 * Статистика завершённых партий (B1): победы человека, победы ИИ и ничьи.
 * {@code total = humanWins + aiWins + draws}.
 */
public record TournamentStatsResponse(
        long humanWins,
        long aiWins,
        long draws,
        long total
) {
}
