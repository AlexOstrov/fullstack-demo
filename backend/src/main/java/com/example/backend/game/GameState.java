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
}
