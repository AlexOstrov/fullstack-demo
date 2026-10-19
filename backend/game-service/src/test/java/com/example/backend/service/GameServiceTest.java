package com.example.backend.service;

import com.example.backend.config.AiProperties;
import com.example.backend.config.LlmProperties;
import com.example.backend.dto.game.CellDto;
import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Difficulty;
import com.example.backend.dto.game.MoveRequest;
import com.example.backend.dto.game.PlayerColor;
import com.example.backend.dto.game.StartGameRequest;
import com.example.backend.error.ApiErrorCode;
import com.example.backend.error.ApiException;
import com.example.backend.game.Board;
import com.example.backend.game.Color;
import com.example.backend.game.GameEndReason;
import com.example.backend.game.GameState;
import com.example.backend.game.GameStatus;
import com.example.backend.game.Piece;
import com.example.backend.game.ai.AiPreset;
import com.example.backend.game.ai.AiSettings;
import com.example.backend.game.ai.AlphaBetaSearch;
import com.example.backend.game.ai.PositionEvaluator;
import com.example.backend.model.Tournament;
import com.example.backend.repository.TournamentRepository;
import com.example.backend.session.GamePreferences;
import com.example.backend.session.GameRegistry;
import com.example.backend.session.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Оркестрация партий без Spring (ТЗ 5.4): ход игрока, идемпотентность,
 * AI_BUSY/STALE_STATE, сдача/ничья, рестарт, автосохранение результата.
 */
class GameServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC);

    private final GameRegistry registry = new GameRegistry(Duration.ofMinutes(60), CLOCK);
    private final TournamentRepository repository = mock(TournamentRepository.class);
    private final AiSettingsFactory aiSettingsFactory = new AiSettingsFactory(aiProperties());
    private final LlmModelCatalog llmModelCatalog = new LlmModelCatalog(llmProperties());
    private final GameService service = new GameService(
            registry,
            new AlphaBetaSearch(),
            new PositionEvaluator(),
            aiSettingsFactory,
            llmModelCatalog,
            new FallbackCommentProvider(),
            new TournamentRecorder(repository),
            CLOCK);

    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(repository);
    }

    @Test
    void startUsesPresetSettingsAndPlayerColor() {
        GameSession session = service.start(request(PlayerColor.BLACK, Difficulty.MEDIUM));

        assertEquals(Color.BLACK, session.humanColor());
        assertEquals(Color.WHITE, session.aiColor());
        assertEquals(4, session.aiSettings().maxDepth());
        assertEquals(1500, session.aiSettings().timeBudgetMs());
        assertEquals(GameStatus.IN_PROGRESS, session.state().status());
        assertEquals(1, registry.size());
    }

    @Test
    void customDifficultyIsBuiltFromRequest() {
        GameSession session = service.start(
                request(PlayerColor.WHITE, Difficulty.CUSTOM, 3, 900));

        assertEquals(3, session.aiSettings().maxDepth());
        assertEquals(900, session.aiSettings().timeBudgetMs());
    }

    @Test
    void customDifficultyRequiresDepthAndBudget() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.start(request(PlayerColor.WHITE, Difficulty.CUSTOM)));
        assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.code());
    }

    @Test
    void modelOutsideAllowlistIsRejected() {
        StartGameRequest request = new StartGameRequest(PlayerColor.WHITE, Difficulty.EASY,
                null, null, CommentaryStyle.NEUTRAL, "unknown:1b");
        ApiException exception = assertThrows(ApiException.class, () -> service.start(request));
        assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.code());
    }

    @Test
    void humanMoveIsAppliedAndTurnPassesToAi() {
        GameSession session = service.start(request(PlayerColor.WHITE, Difficulty.EASY));

        service.humanMove(session.id(), new MoveRequest(
                List.of(new CellDto(5, 0), new CellDto(4, 1)), 0));

        assertEquals(1, session.state().moveNumber());
        assertEquals(Color.BLACK, session.state().currentPlayer());
        assertEquals(1, session.state().history().size());
    }

    @Test
    void staleClientMoveNumberGivesStaleState() {
        GameSession session = service.start(request(PlayerColor.WHITE, Difficulty.EASY));

        ApiException exception = assertThrows(ApiException.class, () -> service.humanMove(
                session.id(), new MoveRequest(List.of(new CellDto(5, 0), new CellDto(4, 1)), 5)));

        assertEquals(ApiErrorCode.STALE_STATE, exception.code());
    }

    @Test
    void movingWhenAiTurnGivesNotYourTurn() {
        GameSession session = service.start(request(PlayerColor.BLACK, Difficulty.EASY));

        ApiException exception = assertThrows(ApiException.class, () -> service.humanMove(
                session.id(), new MoveRequest(List.of(new CellDto(5, 0), new CellDto(4, 1)), 0)));

        assertEquals(ApiErrorCode.NOT_YOUR_TURN, exception.code());
    }

    @Test
    void invalidPathGivesInvalidMove() {
        GameSession session = service.start(request(PlayerColor.WHITE, Difficulty.EASY));

        ApiException exception = assertThrows(ApiException.class, () -> service.humanMove(
                session.id(), new MoveRequest(List.of(new CellDto(5, 0), new CellDto(3, 0)), 0)));

        assertEquals(ApiErrorCode.INVALID_MOVE, exception.code());
    }

    @Test
    void aiMovePlaysAndAddsFallbackComment() {
        GameSession session = service.start(request(PlayerColor.WHITE, Difficulty.CUSTOM, 1, 200));
        service.humanMove(session.id(), new MoveRequest(
                List.of(new CellDto(5, 0), new CellDto(4, 1)), 0));

        AiMoveOutcome outcome = service.aiMove(session.id());

        assertEquals(2, session.state().moveNumber());
        assertEquals(Color.WHITE, session.state().currentPlayer());
        assertTrue(outcome.comment().fallback());
        assertFalse(outcome.comment().text().isBlank());
        assertEquals(1, session.state().comments().size());
        assertEquals(session.state().lastMove(), session.state().history().get(1));
    }

    @Test
    void aiMoveWhenNotAiTurnGivesNotYourTurn() {
        GameSession session = service.start(request(PlayerColor.WHITE, Difficulty.CUSTOM, 1, 200));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.aiMove(session.id()));

        assertEquals(ApiErrorCode.NOT_YOUR_TURN, exception.code());
    }

    @Test
    void concurrentAiMoveWhileThinkingGivesAiBusy() throws Exception {
        GameSession session = service.start(request(PlayerColor.BLACK, Difficulty.EASY));
        GameSession live = registry.find(session.id()).orElseThrow();

        CountDownLatch thinking = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread ai = new Thread(() -> {
            assertTrue(live.tryBeginAi());
            thinking.countDown();
            awaitQuietly(release);
            live.endAi();
        });
        ai.start();
        assertTrue(thinking.await(2, TimeUnit.SECONDS));

        try {
            ApiException exception = assertThrows(ApiException.class,
                    () -> service.aiMove(session.id()));
            assertEquals(ApiErrorCode.AI_BUSY, exception.code());
            assertEquals(ApiErrorCode.AI_BUSY, assertThrows(ApiException.class,
                    () -> service.humanMove(session.id(), new MoveRequest(
                            List.of(new CellDto(5, 0), new CellDto(4, 1)), 0))).code());
        } finally {
            release.countDown();
            ai.join(2000);
        }
    }

    @Test
    void resignFinishesGameAndPersistsResult() {
        GameSession session = service.start(request(PlayerColor.WHITE, Difficulty.EASY));

        service.resign(session.id());

        assertEquals(GameStatus.BLACK_WON, session.state().status());
        assertEquals(GameEndReason.RESIGN, session.state().endReason());
        verify(repository, times(1)).save(any(Tournament.class));

        ApiException exception = assertThrows(ApiException.class, () -> service.resign(session.id()));
        assertEquals(ApiErrorCode.GAME_FINISHED, exception.code());
        verify(repository, times(1)).save(any(Tournament.class)); // повторной записи нет
    }

    @Test
    void drawIsAcceptedWhileAiIsNotAhead() {
        GameSession session = service.start(request(PlayerColor.WHITE, Difficulty.EASY));

        DrawOutcome outcome = service.draw(session.id());

        assertTrue(outcome.accepted());
        assertEquals(GameStatus.DRAW, session.state().status());
        assertEquals(GameEndReason.DRAW_AGREEMENT, session.state().endReason());
        verify(repository, times(1)).save(any(Tournament.class));
    }

    @Test
    void drawIsDeclinedWhenAiIsClearlyAhead() {
        Board board = Board.empty();
        board.place(7, 0, Piece.WHITE_MAN);
        board.place(0, 1, Piece.BLACK_MAN);
        board.place(0, 3, Piece.BLACK_MAN);
        board.place(0, 5, Piece.BLACK_MAN);
        GameState state = GameState.ofPosition(board, Color.WHITE, Color.WHITE);
        GameSession session = registry.register(new GameSession(state,
                new GamePreferences(PlayerColor.WHITE, Difficulty.EASY, null, null,
                        CommentaryStyle.NEUTRAL, "qwen2.5:7b-instruct"),
                AiSettings.of(AiPreset.EASY), CLOCK.instant()));

        DrawOutcome outcome = service.draw(session.id());

        assertFalse(outcome.accepted());
        assertEquals(GameStatus.IN_PROGRESS, session.state().status());
        verify(repository, times(0)).save(any(Tournament.class));
    }

    @Test
    void restartKeepsPreferencesButCreatesNewGame() {
        GameSession session = service.start(request(PlayerColor.BLACK, Difficulty.HARD));

        GameSession restarted = service.restart(session.id(), null);

        assertNotEquals(session.id(), restarted.id());
        assertEquals(PlayerColor.BLACK, restarted.preferences().playerColor());
        assertEquals(Difficulty.HARD, restarted.preferences().difficulty());
        assertEquals(GameStatus.IN_PROGRESS, restarted.state().status());
        assertEquals(0, restarted.state().moveNumber());
        assertEquals(2, registry.size());
    }

    @Test
    void unknownGameGivesGameNotFound() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.get(UUID.randomUUID()));
        assertEquals(ApiErrorCode.GAME_NOT_FOUND, exception.code());
    }

    private static StartGameRequest request(PlayerColor color, Difficulty difficulty) {
        return request(color, difficulty, null, null);
    }

    private static StartGameRequest request(PlayerColor color, Difficulty difficulty,
                                            Integer depth, Integer budget) {
        return new StartGameRequest(color, difficulty, depth, budget,
                CommentaryStyle.FRIENDLY_MASTER, "qwen2.5:7b-instruct");
    }

    private static AiProperties aiProperties() {
        return new AiProperties(1024, Map.of(
                "easy", new AiProperties.PresetSettings(2, 500),
                "medium", new AiProperties.PresetSettings(4, 1500),
                "hard", new AiProperties.PresetSettings(6, 3000)));
    }

    private static LlmProperties llmProperties() {
        return new LlmProperties("http://localhost:11434", "qwen2.5:7b-instruct",
                List.of("qwen2.5:7b-instruct", "qwen2.5:3b-instruct"),
                8000, 1, 0.7, 2048, true);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
