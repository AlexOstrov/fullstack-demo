package com.example.backend.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Состояние партии, in-memory (ТЗ 4.1).
 * Живые партии в БД не хранятся (ADR-0001); изоляция — per-game блокировка (ТЗ 4.2).
 */
public final class GameState {

    /** Порог ничьей «только дамками»: 15 ходов каждого игрока = 30 полуходов (ТЗ 3.6). */
    private static final int KING_ONLY_DRAW_HALF_MOVES = 30;

    private final UUID id;
    private final Board board;
    private final Color humanColor;
    private final Color aiColor;

    private Color currentPlayer;
    private GameStatus status;
    private long moveNumber;
    private Move lastMove;
    private final List<Move> history = new ArrayList<>();
    private final List<LlmComment> comments = new ArrayList<>();
    private final Map<String, Integer> positionRepetitions = new HashMap<>();
    private int noCaptureKingMoves;
    private long version;

    private GameState(UUID id, Board board, Color humanColor) {
        this.id = Objects.requireNonNull(id, "id");
        this.board = Objects.requireNonNull(board, "board");
        this.humanColor = Objects.requireNonNull(humanColor, "humanColor");
        this.aiColor = humanColor.opposite();
        this.currentPlayer = Color.WHITE; // белые ходят первыми (ТЗ 3.1)
        this.status = GameStatus.IN_PROGRESS;
        this.moveNumber = 0;
        this.lastMove = null;
        this.noCaptureKingMoves = 0;
        this.version = 0;
        this.positionRepetitions.put(positionKey(), 1);
    }

    /** Новая партия: начальная расстановка, ход белых, статус IN_PROGRESS. */
    public static GameState newGame(Color humanColor) {
        return new GameState(UUID.randomUUID(), Board.initialPosition(), humanColor);
    }

    /** Ключ позиции «расстановка + игрок, чей ход» — для троекратного повторения (ТЗ 3.6). */
    public String positionKey() {
        return board.positionKey() + ':' + currentPlayer;
    }

    public UUID id() {
        return id;
    }

    public Board board() {
        return board;
    }

    public Color humanColor() {
        return humanColor;
    }

    public Color aiColor() {
        return aiColor;
    }

    public Color currentPlayer() {
        return currentPlayer;
    }

    public GameStatus status() {
        return status;
    }

    public long moveNumber() {
        return moveNumber;
    }

    /** Путь и тип последнего хода; null до первого хода. */
    public Move lastMove() {
        return lastMove;
    }

    public List<Move> history() {
        return Collections.unmodifiableList(history);
    }

    public List<LlmComment> comments() {
        return Collections.unmodifiableList(comments);
    }

    /** Таблица повторений позиций (снимок). */
    public Map<String, Integer> positionRepetitions() {
        return Collections.unmodifiableMap(new HashMap<>(positionRepetitions));
    }

    /** Счётчик ходов только дамками без взятий (для ничьей «30 полуходов», ТЗ 3.6). */
    public int noCaptureKingMoves() {
        return noCaptureKingMoves;
    }

    /** Монотонно растущая версия состояния — защита от устаревших запросов (ТЗ 4.1). */
    public long version() {
        return version;
    }

    // --- Применение ходов (ТЗ 4.4–4.5) ---

    /**
     * Валидирует путь как один ход (ТЗ 4.4) и применяет его (ТЗ 4.5):
     * снятие взятых шашек по мере прыжка, немедленное превращение (ТЗ 3.5),
     * обновление счётчиков повторений и «дамки без взятий», смена игрока.
     * Определение конца партии — отдельный шаг (ТЗ 3.6, S1.5).
     *
     * @throws InvalidMoveException если партия завершена или ход недопустим
     */
    public void applyMove(List<Cell> path) {
        checkInProgress("ходить");

        Move move = MoveValidator.validate(board, currentPlayer, path);
        Piece startPiece = board.at(path.get(0).row(), path.get(0).col());

        MoveApplier.applyInPlace(board, move); // снятие жертв по мере прыжка, превращение (ТЗ 3.5, 4.5)

        history.add(move);
        lastMove = move;
        moveNumber++;
        version++;
        // счётчик «дамки без взятий»: только ход дамки без боя (ТЗ 3.6)
        boolean kingQuietMove = startPiece.isKing() && move.type() == MoveType.NORMAL;
        noCaptureKingMoves = kingQuietMove ? noCaptureKingMoves + 1 : 0;
        currentPlayer = currentPlayer.opposite();
        positionRepetitions.merge(positionKey(), 1, Integer::sum);
        checkGameEnd();
    }

    /**
     * Сдача игрока (ТЗ 3.6): фиксирует поражение сдавшегося.
     * Учёт предложений и ответов на ничью — сервисный слой API (Sprint 3).
     */
    public void resign(Color player) {
        checkInProgress("сдаться");
        status = player == Color.WHITE ? GameStatus.BLACK_WON : GameStatus.WHITE_WON;
        version++;
    }

    /** Ничья по согласию (ТЗ 3.6) — когда согласие получено от обоих игроков. */
    public void drawByAgreement() {
        checkInProgress("зафиксировать ничью");
        status = GameStatus.DRAW;
        version++;
    }

    private void checkInProgress(String action) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new InvalidMoveException(
                    "нельзя " + action + ": партия уже завершена (статус " + status + ")");
        }
    }

    /**
     * Определение конца партии после хода (ТЗ 3.6):
     * у следующего игрока нет допустимых ходов — он проигрывает;
     * ничья — троекратное повторение позиции либо 30 полуходов
     * подряд только дамками без взятий. Сдача и ничья по согласию — API-слой.
     */
    private void checkGameEnd() {
        if (MoveValidator.analyze(board, currentPlayer).moves().isEmpty()) {
            status = currentPlayer == Color.WHITE ? GameStatus.BLACK_WON : GameStatus.WHITE_WON;
        } else if (positionRepetitions.getOrDefault(positionKey(), 0) >= 3) {
            status = GameStatus.DRAW; // троекратное повторение
        } else if (noCaptureKingMoves >= KING_ONLY_DRAW_HALF_MOVES) {
            status = GameStatus.DRAW; // 15 ходов каждого игрока только дамками
        }
    }

    /** Состояние из произвольной позиции — для тестов правил (S1.4). */
    static GameState ofPosition(Board board, Color humanColor, Color currentPlayer) {
        GameState state = new GameState(UUID.randomUUID(), board, humanColor);
        state.currentPlayer = currentPlayer;
        state.positionRepetitions.clear();
        state.positionRepetitions.put(state.positionKey(), 1);
        return state;
    }
}
