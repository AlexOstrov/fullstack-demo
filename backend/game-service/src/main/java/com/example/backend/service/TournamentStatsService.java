package com.example.backend.service;

import com.example.backend.dto.game.TournamentStatsResponse;
import com.example.backend.dto.game.WinnerCount;
import com.example.backend.repository.TournamentRepository;
import org.springframework.stereotype.Service;

/**
 * Агрегация результатов завершённых партий для блока статистики (B1).
 * Источник — таблица {@code tournament}, куда результаты пишет
 * {@link TournamentRecorder} (best-effort).
 */
@Service
public class TournamentStatsService {

    private final TournamentRepository repository;

    public TournamentStatsService(TournamentRepository repository) {
        this.repository = repository;
    }

    /** Сводка побед человека/ИИ и ничьих. */
    public TournamentStatsResponse stats() {
        long humanWins = 0;
        long aiWins = 0;
        long draws = 0;
        for (WinnerCount count : repository.countByWinner()) {
            switch (count.winner()) {
                case "HUMAN" -> humanWins += count.total();
                case "AI" -> aiWins += count.total();
                case "DRAW" -> draws += count.total();
                default -> {
                    // Неизвестное значение не должно ломать статистику.
                }
            }
        }
        return new TournamentStatsResponse(humanWins, aiWins, draws, humanWins + aiWins + draws);
    }
}
