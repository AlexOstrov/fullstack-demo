package com.example.backend.service;

import com.example.backend.dto.game.CellDto;
import com.example.backend.dto.game.DrawResponse;
import com.example.backend.dto.game.MoveRequest;
import com.example.backend.dto.game.StartGameRequest;
import com.example.backend.error.ApiErrorCode;
import com.example.backend.error.ApiException;
import com.example.backend.game.Cell;
import com.example.backend.game.GameState;
import com.example.backend.game.InvalidMoveException;
import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.game.ai.AiResult;
import com.example.backend.game.ai.AiSettings;
import com.example.backend.game.ai.AlphaBetaSearch;
import com.example.backend.game.ai.PositionEvaluator;
import com.example.backend.session.GamePreferences;
import com.example.backend.session.GameRegistry;
import com.example.backend.session.GameSession;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Оркестрация партий (ТЗ 5): старт/ходы/ИИ/сдача/ничья/рестарт.
 * <p>Гарантии: любая партия обрабатывается под собственным per-game lock (ТЗ 4.2),
 * повторный {@code ai-move} во время счёта ИИ даёт {@code AI_BUSY}, устаревший
 * {@code clientMoveNumber} — {@code STALE_STATE} (ТЗ 5.4). Ход ИИ считается
 * с историей повторений партии как есть — контракт R-1 (ADR-0014).
 */
@Service
public class GameService {

    /**
     * Порог согласия ИИ на ничью: ИИ принимает предложение, если оценка
     * (с его стороны) не лучше чем на половину простой шашки. Политика
     * детерминирована и не задаётся ТЗ — см. ADR-0015.
     */
    static final int DRAW_ACCEPT_THRESHOLD = 50;

    private final GameRegistry registry;
    private final AlphaBetaSearch search;
    private final PositionEvaluator evaluator;
    private final AiSettingsFactory aiSettingsFactory;
    private final LlmModelCatalog llmModels;
    private final CommentProvider commentProvider;
    private final TournamentRecorder tournamentRecorder;
    private final LlmCommentRecorder llmCommentRecorder;
    private final Clock clock;

    public GameService(GameRegistry registry,
                       AlphaBetaSearch search,
                       PositionEvaluator evaluator,
                       AiSettingsFactory aiSettingsFactory,
                       LlmModelCatalog llmModels,
                       CommentProvider commentProvider,
                       TournamentRecorder tournamentRecorder,
                       LlmCommentRecorder llmCommentRecorder,
                       Clock clock) {
        this.registry = registry;
        this.search = search;
        this.evaluator = evaluator;
        this.aiSettingsFactory = aiSettingsFactory;
        this.llmModels = llmModels;
        this.commentProvider = commentProvider;
        this.tournamentRecorder = tournamentRecorder;
        this.llmCommentRecorder = llmCommentRecorder;
        this.clock = clock;
    }

    /** Создать партию с настройками игрока (ТЗ 5, {@code POST /start}). */
    public GameSession start(StartGameRequest request) {
        GamePreferences preferences = validate(request);
        AiSettings settings = aiSettingsFactory.resolve(
                preferences.difficulty(), preferences.aiDepth(), preferences.aiTimeBudgetMs());
        GameState state = GameState.newGame(preferences.playerColor().toColor());
        return registry.register(new GameSession(state, preferences, settings, clock.instant()));
    }

    /** Текущее состояние партии (ТЗ 5, {@code GET /{id}}). */
    public GameSession get(UUID id) {
        return requireSession(id);
    }

    /** Ход игрока: валидация, применение, конец партии (ТЗ 5.4). */
    public GameSession humanMove(UUID id, MoveRequest request) {
        GameSession session = requireSession(id);
        if (session.aiThinking()) {
            throw new ApiException(ApiErrorCode.AI_BUSY,
                    "ИИ считает ход — повторите запрос позже");
        }
        session.lock().lock();
        try {
            GameState state = session.state();
            requireInProgress(state);
            if (state.moveNumber() != request.clientMoveNumber()) {
                throw new ApiException(ApiErrorCode.STALE_STATE,
                        "Устаревшее состояние: ожидался ход №" + state.moveNumber()
                                + ", получен clientMoveNumber=" + request.clientMoveNumber());
            }
            if (state.currentPlayer() != session.humanColor()) {
                throw new ApiException(ApiErrorCode.NOT_YOUR_TURN, "Сейчас ход ИИ");
            }
            applyMove(state, request.path());
            settle(session);
            return session;
        } finally {
            session.lock().unlock();
        }
    }

    /** Ход ИИ: минимакс → ход → комментарий → новое состояние (ТЗ 5). */
    public AiMoveOutcome aiMove(UUID id) {
        GameSession session = requireSession(id);
        if (!session.tryBeginAi()) {
            throw new ApiException(ApiErrorCode.AI_BUSY,
                    "ИИ уже считает ход в этой партии");
        }
        try {
            session.lock().lock();
            try {
                GameState state = session.state();
                requireInProgress(state);
                if (state.currentPlayer() != session.aiColor()) {
                    throw new ApiException(ApiErrorCode.NOT_YOUR_TURN, "Сейчас не ход ИИ");
                }
                // Контракт R-1: карта повторений включает текущую позицию — передаём как есть.
                AiResult result = search.findBestMove(
                        state.board(), session.aiColor(),
                        state.positionRepetitions(), state.noCaptureKingMoves(),
                        session.aiSettings());
                Move move = result.move();
                if (move == null) {
                    throw new IllegalStateException(
                            "У ИИ нет допустимых ходов, но партия в статусе IN_PROGRESS");
                }
                state.applyMove(move.path());
                LlmComment comment = commentProvider.comment(session, move);
                state.addComment(comment);
                llmCommentRecorder.record(session, comment);
                settle(session);
                return new AiMoveOutcome(session, comment);
            } finally {
                session.lock().unlock();
            }
        } finally {
            session.endAi();
        }
    }

    /** Сдача игрока (ТЗ 3.6): поражение сдавшегося. */
    public GameSession resign(UUID id) {
        GameSession session = requireSession(id);
        session.lock().lock();
        try {
            GameState state = session.state();
            requireInProgress(state);
            state.resign(session.humanColor());
            settle(session);
            return session;
        } finally {
            session.lock().unlock();
        }
    }

    /** Предложение ничьей: решает ИИ (ТЗ 5). */
    public DrawOutcome draw(UUID id) {
        GameSession session = requireSession(id);
        session.lock().lock();
        try {
            GameState state = session.state();
            requireInProgress(state);
            boolean accepted = evaluator.evaluate(state.board(), session.aiColor())
                    <= DRAW_ACCEPT_THRESHOLD;
            if (accepted) {
                state.drawByAgreement();
                settle(session);
            }
            return new DrawOutcome(accepted, session);
        } finally {
            session.lock().unlock();
        }
    }

    /**
     * Новая партия вместо указанной (ТЗ 5): при пустом теле сохраняются
     * прежние настройки. Возвращается партия с новым {@code gameId}, чтобы
     * результат прежней партии оставался уникальным в {@code tournament}.
     */
    public GameSession restart(UUID id, StartGameRequest request) {
        GameSession previous = requireSession(id);
        StartGameRequest effective = request != null ? request : toRequest(previous.preferences());
        return start(effective);
    }

    private GamePreferences validate(StartGameRequest request) {
        llmModels.requireAllowed(request.llmModel());
        return new GamePreferences(
                request.playerColor(), request.difficulty(),
                request.aiDepth(), request.aiTimeBudgetMs(),
                request.commentaryStyle(), request.llmModel());
    }

    private StartGameRequest toRequest(GamePreferences preferences) {
        return new StartGameRequest(
                preferences.playerColor(), preferences.difficulty(),
                preferences.aiDepth(), preferences.aiTimeBudgetMs(),
                preferences.commentaryStyle(), preferences.llmModel());
    }

    private void applyMove(GameState state, List<CellDto> path) {
        List<Cell> cells = path.stream().map(CellDto::toCell).toList();
        try {
            state.applyMove(cells);
        } catch (InvalidMoveException exception) {
            throw new ApiException(ApiErrorCode.INVALID_MOVE, exception.getMessage());
        }
    }

    private GameSession requireSession(UUID id) {
        return registry.find(id).orElseThrow(() -> new ApiException(
                ApiErrorCode.GAME_NOT_FOUND, "Партия не найдена: " + id));
    }

    private void requireInProgress(GameState state) {
        if (state.status().isTerminal()) {
            throw new ApiException(ApiErrorCode.GAME_FINISHED,
                    "Партия завершена: " + state.status() + " (" + state.endReason() + ")");
        }
    }

    /** Зафиксировать завершение и (best-effort) сохранить результат (ТЗ 4.7). */
    private void settle(GameSession session) {
        if (session.state().status().isTerminal() && session.markFinished(clock.instant())) {
            tournamentRecorder.record(session);
        }
    }
}
