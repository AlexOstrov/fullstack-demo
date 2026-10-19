package com.example.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Результат завершённой партии (ТЗ 4.7, таблица {@code tournament}).
 * Запись создаётся автоматически при завершении партии (best-effort).
 */
@Entity
@Table(name = "tournament")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false, unique = true)
    private UUID gameId;

    /** {@code HUMAN} / {@code AI} / {@code DRAW}. */
    @Column(nullable = false, length = 8)
    private String winner;

    /** {@code WHITE} / {@code BLACK}; {@code null} при ничьей. */
    @Column(name = "winner_color", length = 8)
    private String winnerColor;

    /** Цвет игрока — для будущей статистики (ТЗ B1). */
    @Column(name = "human_color", nullable = false, length = 8)
    private String humanColor;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "moves_count", nullable = false)
    private int movesCount;

    /** {@code NO_MOVES} / {@code RESIGN} / {@code DRAW_REPETITION} / {@code DRAW_KINGS} / {@code DRAW_AGREEMENT}. */
    @Column(name = "end_reason", nullable = false, length = 24)
    private String endReason;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    protected Tournament() {
    }

    public Tournament(UUID gameId, String winner, String winnerColor, String humanColor,
                      int durationSeconds, int movesCount, String endReason, Instant finishedAt) {
        this.gameId = gameId;
        this.winner = winner;
        this.winnerColor = winnerColor;
        this.humanColor = humanColor;
        this.durationSeconds = durationSeconds;
        this.movesCount = movesCount;
        this.endReason = endReason;
        this.finishedAt = finishedAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getGameId() {
        return gameId;
    }

    public String getWinner() {
        return winner;
    }

    public String getWinnerColor() {
        return winnerColor;
    }

    public String getHumanColor() {
        return humanColor;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getMovesCount() {
        return movesCount;
    }

    public String getEndReason() {
        return endReason;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
