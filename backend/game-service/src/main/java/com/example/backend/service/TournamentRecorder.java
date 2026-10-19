package com.example.backend.service;

import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.game.GameStatus;
import com.example.backend.model.Tournament;
import com.example.backend.repository.TournamentRepository;
import com.example.backend.session.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Автосохранение результата завершённой партии в {@code tournament} (ТЗ 4.7).
 * <p>Best-effort: сбой БД логируется и не ломает партию (ADR-0004).
 * Повторная запись исключается флагом сессии ({@code game_id} также UNIQUE).
 */
@Service
public class TournamentRecorder {

    private static final Logger log = LoggerFactory.getLogger(TournamentRecorder.class);

    private final TournamentRepository repository;

    public TournamentRecorder(TournamentRepository repository) {
        this.repository = repository;
    }

    /** Сохранить результат, если партия завершена. Сбой БД не пробрасывается. */
    public void record(GameSession session) {
        GameState state = session.state();
        if (!state.status().isTerminal()) {
            return;
        }
        if (!session.markResultSaved()) {
            return; // уже сохранено (идемпотентность)
        }
        try {
            repository.save(toEntity(session));
            log.info("Результат партии {} сохранён: {} / {} ({} ходов)",
                    session.id(), state.status(), state.endReason(), state.moveNumber());
        } catch (RuntimeException exception) {
            session.clearResultSaved();
            log.warn("Не удалось сохранить результат партии {} (best-effort): {}",
                    session.id(), exception.getMessage());
        }
    }

    private Tournament toEntity(GameSession session) {
        GameState state = session.state();
        Instant finishedAt = session.finishedAt() != null ? session.finishedAt() : Instant.now();
        int durationSeconds = (int) Math.max(0,
                Duration.between(session.startedAt(), finishedAt).getSeconds());
        Color winnerColor = state.status().winnerColor();
        return new Tournament(
                session.id(),
                winner(state.status(), winnerColor, session.humanColor()),
                winnerColor == null ? null : winnerColor.name(),
                session.humanColor().name(),
                durationSeconds,
                (int) state.moveNumber(),
                state.endReason().name(),
                finishedAt);
    }

    private static String winner(GameStatus status, Color winnerColor, Color humanColor) {
        if (status == GameStatus.DRAW) {
            return "DRAW";
        }
        return winnerColor == humanColor ? "HUMAN" : "AI";
    }
}
