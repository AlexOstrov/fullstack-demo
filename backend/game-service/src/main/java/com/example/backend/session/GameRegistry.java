package com.example.backend.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр живых партий (ТЗ 4.2): {@link ConcurrentHashMap} + TTL неактивных партий.
 * <p>Чистая инфраструктура без Spring — создаётся как bean в {@code GameConfiguration},
 * что позволяет тестировать TTL и изоляцию с управляемыми часами ({@link Clock}).
 * <p>Однопроцессный режим: в БД живые партии не хранятся (ADR-0001).
 */
public final class GameRegistry {

    private final ConcurrentHashMap<UUID, GameSession> sessions = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    public GameRegistry(Duration ttl, Clock clock) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL партии должен быть положительным: " + ttl);
        }
        this.ttl = ttl;
        this.clock = clock;
    }

    /** Зарегистрировать новую партию. */
    public GameSession register(GameSession session) {
        sessions.put(session.id(), session);
        return session;
    }

    /** Партия по id с отметкой активности (TTL продлевается); пусто, если не найдена/вытеснена. */
    public Optional<GameSession> find(UUID id) {
        GameSession session = sessions.get(id);
        if (session != null) {
            session.touch(clock.instant());
        }
        return Optional.ofNullable(session);
    }

    /** Партия по id без отметки активности. */
    public Optional<GameSession> peek(UUID id) {
        return Optional.ofNullable(sessions.get(id));
    }

    /** Убрать партию из реестра; {@code true}, если партия была. */
    public boolean remove(UUID id) {
        return sessions.remove(id) != null;
    }

    /** Число живых партий. */
    public int size() {
        return sessions.size();
    }

    /** TTL неактивной партии. */
    public Duration ttl() {
        return ttl;
    }

    /**
     * Вытеснить партии, к которым не обращались дольше TTL (ТЗ 4.2).
     * Вызывается фоновой задачей; отдельный метод — для тестов.
     *
     * @return список вытесненных партий
     */
    public List<GameSession> evictExpired() {
        Instant deadline = clock.instant().minus(ttl);
        List<GameSession> expired = sessions.values().stream()
                .filter(session -> !session.lastAccess().isAfter(deadline))
                .toList();
        expired.forEach(session -> sessions.remove(session.id(), session));
        return expired;
    }
}
