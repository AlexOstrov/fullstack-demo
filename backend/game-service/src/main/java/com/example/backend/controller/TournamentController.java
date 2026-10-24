package com.example.backend.controller;

import com.example.backend.dto.game.TournamentStatsResponse;
import com.example.backend.service.TournamentStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Статистика партий (B1, ТЗ 16): {@code GET /api/tournament}. */
@RestController
@RequestMapping("/api/tournament")
public class TournamentController {

    private final TournamentStatsService statsService;

    public TournamentController(TournamentStatsService statsService) {
        this.statsService = statsService;
    }

    /** Сводка побед человека/ИИ и ничьих по завершённым партиям. */
    @GetMapping
    public TournamentStatsResponse stats() {
        return statsService.stats();
    }
}
