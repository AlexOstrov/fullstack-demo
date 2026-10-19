package com.example.backend.session;

import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.game.ai.AiSettings;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Живая партия: состояние движка (ТЗ 4.1) + настройки + изоляция (ТЗ 4.2).
 * <p>Изоляция по партии — собственный {@link ReentrantLock}; одновременные
 * {@code move}/{@code ai-move} по одной партии сериализуются, разные партии
 * не блокируют друг друга.
 * <p>Флаг {@code aiThinking} даёт быстрый ответ {@code AI_BUSY} на повторный
 * {@code ai-move}, пока ИИ считает (ТЗ 5.4), не дожидаясь освобождения блокировки.
 */
public final class GameSession {

    private final GameState state;
    private final GamePreferences preferences;
    private final AiSettings aiSettings;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean aiThinking = new AtomicBoolean(false);
    private final AtomicBoolean resultSaved = new AtomicBoolean(false);
    private final Instant startedAt;

    private volatile Instant lastAccess;
    private volatile Instant finishedAt;

    public GameSession(GameState state, GamePreferences preferences, AiSettings aiSettings,
                       Instant startedAt) {
        this.state = Objects.requireNonNull(state, "state");
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.aiSettings = Objects.requireNonNull(aiSettings, "aiSettings");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.lastAccess = startedAt;
    }

    public UUID id() {
        return state.id();
    }

    public GameState state() {
        return state;
    }

    public GamePreferences preferences() {
        return preferences;
    }

    public AiSettings aiSettings() {
        return aiSettings;
    }

    public Color humanColor() {
        return preferences.playerColor().toColor();
    }

    public Color aiColor() {
        return humanColor().opposite();
    }

    public Instant startedAt() {
        return startedAt;
    }

    /** Момент последнего обращения к партии (для TTL-вытеснения, ТЗ 4.2). */
    public Instant lastAccess() {
        return lastAccess;
    }

    /** Отметить активность партии (TTL отсчитывается от последнего обращения). */
    public void touch(Instant now) {
        this.lastAccess = Objects.requireNonNull(now, "now");
    }

    /** Момент завершения партии; {@code null}, пока партия идёт. */
    public Instant finishedAt() {
        return finishedAt;
    }

    /**
     * Зафиксировать момент завершения (однократно).
     *
     * @return {@code true}, если момент был проставлен именно этим вызовом
     */
    public boolean markFinished(Instant now) {
        if (finishedAt != null) {
            return false;
        }
        finishedAt = Objects.requireNonNull(now, "now");
        return true;
    }

    /** Заблокировать партию на время обработки запроса (ТЗ 4.2). */
    public ReentrantLock lock() {
        return lock;
    }

    /** Начать счёт ИИ; {@code false}, если ИИ уже считает эту партию (ТЗ 5.4). */
    public boolean tryBeginAi() {
        return aiThinking.compareAndSet(false, true);
    }

    /** Завершить счёт ИИ. */
    public void endAi() {
        aiThinking.set(false);
    }

    /** Считает ли ИИ ход в этой партии прямо сейчас. */
    public boolean aiThinking() {
        return aiThinking.get();
    }

    /** Отметить, что результат партии сохранён в БД (защита от повторной записи). */
    public boolean markResultSaved() {
        return resultSaved.compareAndSet(false, true);
    }

    /** Снять отметку о сохранении (при сбое БД — запись можно повторить). */
    public void clearResultSaved() {
        resultSaved.set(false);
    }
}
