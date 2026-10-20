package com.example.backend.service;

import com.example.backend.dto.game.CellDto;
import com.example.backend.dto.game.Difficulty;
import com.example.backend.dto.game.MoveRequest;
import com.example.backend.dto.game.PlayerColor;
import com.example.backend.dto.game.StartGameRequest;
import com.example.backend.error.ApiErrorCode;
import com.example.backend.error.ApiException;
import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.session.GameRegistry;
import com.example.backend.session.GameSession;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Стресс-тест конкурентности партий (Sprint 8, S8.2).
 * Проверяет, что одновременные запросы к одной партии не ломают её состояние
 * (per-game lock работает корректно, AI_BUSY возвращается при конфликте).
 */
class ConcurrentAiMoveStressTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void concurrentAiMovesSamePartyDoNotCorruptState() throws Exception {
        GameRegistry registry = new GameRegistry(java.time.Duration.ofMinutes(60), CLOCK);
        GameService service = new GameService(
            registry,
            new com.example.backend.game.ai.AlphaBetaSearch(),
            new com.example.backend.game.ai.PositionEvaluator(),
            new com.example.backend.service.AiSettingsFactory(
                new com.example.backend.config.AiProperties(1024, java.util.Map.of(
                    "easy", new com.example.backend.config.AiProperties.PresetSettings(2, 500),
                    "medium", new com.example.backend.config.AiProperties.PresetSettings(4, 1500),
                    "hard", new com.example.backend.config.AiProperties.PresetSettings(6, 3000)))),
            new com.example.backend.service.LlmModelCatalog(
                new com.example.backend.config.LlmProperties(
                    "http://localhost:11434", "qwen2.5:7b-instruct",
                    java.util.List.of("qwen2.5:7b-instruct"), 8000, 1, 0.7, 2048, true)),
            new com.example.backend.service.FallbackCommentProvider(),
            new com.example.backend.service.TournamentRecorder(null),
            new com.example.backend.service.LlmCommentRecorder(null),
            CLOCK);

        // Создаём партию
        GameSession session = service.start(new StartGameRequest(
            PlayerColor.WHITE, Difficulty.EASY, null, null,
            com.example.backend.dto.game.CommentaryStyle.NEUTRAL, "qwen2.5:7b-instruct"));
        java.util.UUID gameId = session.id();

        // Игрок делает первый ход
        service.humanMove(gameId, new MoveRequest(
            java.util.List.of(new CellDto(5, 0), new CellDto(4, 1)), 0));

        int numThreads = 20;
        int numRequestsPerThread = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger aiBusyCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < numRequestsPerThread; i++) {
                        try {
                            service.aiMove(gameId);
                            successCount.incrementAndGet();
                        } catch (ApiException e) {
                            if (e.code() == ApiErrorCode.AI_BUSY) {
                                aiBusyCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        try {
            doneLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Проверяем, что партия не сломана
        GameSession finalSession = registry.find(gameId).orElseThrow();
        GameState state = finalSession.state();

        // moveNumber должен быть > 1 (первый ход игрока + хотя бы один ход ИИ)
        assertThat(state.moveNumber()).isGreaterThan(1);
        // Статус должен быть корректным
        assertThat(state.status()).isIn(com.example.backend.game.GameStatus.IN_PROGRESS,
            com.example.backend.game.GameStatus.WHITE_WON,
            com.example.backend.game.GameStatus.BLACK_WON,
            com.example.backend.game.GameStatus.DRAW);
        // Если партия ещё не закончилась, очередь хода должна быть корректной
        if (state.status() == com.example.backend.game.GameStatus.IN_PROGRESS) {
            assertThat(state.currentPlayer()).isEqualTo(session.aiColor().opposite());
        }

        System.out.printf(
            "Concurrent test: %d threads x %d requests = %d total. Success: %d, AI_BUSY: %d, Errors: %d. Final moveNumber=%d%n",
            numThreads, numRequestsPerThread, numThreads * numRequestsPerThread,
            successCount.get(), aiBusyCount.get(), errorCount.get(), state.moveNumber());

        // Ход ИИ должен быть выполнен хотя бы один раз
        assertThat(successCount.get()).isGreaterThan(0);
    }

    @Test
    void concurrentHumanMovesSamePartyOnlyOneSucceeds() throws Exception {
        GameRegistry registry = new GameRegistry(java.time.Duration.ofMinutes(60), CLOCK);
        GameService service = new GameService(
            registry,
            new com.example.backend.game.ai.AlphaBetaSearch(),
            new com.example.backend.game.ai.PositionEvaluator(),
            new com.example.backend.service.AiSettingsFactory(
                new com.example.backend.config.AiProperties(1024, java.util.Map.of(
                    "easy", new com.example.backend.config.AiProperties.PresetSettings(2, 500),
                    "medium", new com.example.backend.config.AiProperties.PresetSettings(4, 1500),
                    "hard", new com.example.backend.config.AiProperties.PresetSettings(6, 3000)))),
            new com.example.backend.service.LlmModelCatalog(
                new com.example.backend.config.LlmProperties(
                    "http://localhost:11434", "qwen2.5:7b-instruct",
                    java.util.List.of("qwen2.5:7b-instruct"), 8000, 1, 0.7, 2048, true)),
            new com.example.backend.service.FallbackCommentProvider(),
            new com.example.backend.service.TournamentRecorder(null),
            new com.example.backend.service.LlmCommentRecorder(null),
            CLOCK);

        GameSession session = service.start(new StartGameRequest(
            PlayerColor.WHITE, Difficulty.EASY, null, null,
            com.example.backend.dto.game.CommentaryStyle.NEUTRAL, "qwen2.5:7b-instruct"));
        java.util.UUID gameId = session.id();

        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger staleCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    service.humanMove(gameId, new MoveRequest(
                        java.util.List.of(new CellDto(5, 0), new CellDto(4, 1)), 0));
                    successCount.incrementAndGet();
                } catch (ApiException e) {
                    if (e.code() == ApiErrorCode.STALE_STATE || e.code() == ApiErrorCode.NOT_YOUR_TURN) {
                        staleCount.incrementAndGet();
                    }
                }
            });
        }

        startLatch.countDown();
        try {
            doneLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Только один ход игрока должен пройти, остальные — STALE_STATE или NOT_YOUR_TURN
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(staleCount.get()).isEqualTo(numThreads - 1);

        System.out.printf(
            "Concurrent human moves: %d threads. Success: %d, STALE/NOT_YOUR_TURN: %d%n",
            numThreads, successCount.get(), staleCount.get());
    }
}
