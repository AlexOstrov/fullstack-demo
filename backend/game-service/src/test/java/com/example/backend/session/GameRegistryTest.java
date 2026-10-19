package com.example.backend.session;

import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Difficulty;
import com.example.backend.dto.game.PlayerColor;
import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.game.ai.AiPreset;
import com.example.backend.game.ai.AiSettings;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Реестр партий (ТЗ 4.2): {@code ConcurrentHashMap}, TTL неактивных сессий,
 * изоляция по партии (per-game lock).
 */
class GameRegistryTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-18T10:00:00Z"));
    private final GameRegistry registry = new GameRegistry(Duration.ofMinutes(60), clock);

    @Test
    void registerFindAndRemove() {
        GameSession session = newSession(Color.WHITE);

        assertSame(session, registry.register(session));
        assertEquals(1, registry.size());
        assertSame(session, registry.find(session.id()).orElseThrow());

        assertTrue(registry.remove(session.id()));
        assertFalse(registry.find(session.id()).isPresent());
        assertEquals(0, registry.size());
    }

    @Test
    void unknownIdIsEmpty() {
        assertTrue(registry.peek(java.util.UUID.randomUUID()).isEmpty());
    }

    @Test
    void accessRefreshesTtl() {
        GameSession session = newSession(Color.WHITE);
        registry.register(session);

        clock.advance(Duration.ofMinutes(50));
        registry.find(session.id()); // активность продлевает TTL

        clock.advance(Duration.ofMinutes(50)); // от старта 100 минут, от доступа — 50
        assertEquals(List.of(), registry.evictExpired());
        assertEquals(1, registry.size());
    }

    @Test
    void idleSessionIsEvictedAfterTtl() {
        GameSession session = newSession(Color.WHITE);
        registry.register(session);

        clock.advance(Duration.ofMinutes(60).plusSeconds(1));
        List<GameSession> evicted = registry.evictExpired();

        assertEquals(List.of(session), evicted);
        assertEquals(0, registry.size());
    }

    @Test
    void locksAreIsolatedPerGame() {
        GameSession first = registry.register(newSession(Color.WHITE));
        GameSession second = registry.register(newSession(Color.BLACK));

        first.lock().lock();
        try {
            assertTrue(second.lock().tryLock(), "блокировка одной партии не блокирует другую");
            second.lock().unlock();
            assertTrue(first.lock().isLocked(), "партия удерживается своим запросом");
        } finally {
            first.lock().unlock();
        }
    }

    @Test
    void nonPositiveTtlIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameRegistry(Duration.ZERO, clock));
        assertThrows(IllegalArgumentException.class,
                () -> new GameRegistry(Duration.ofMinutes(-1), clock));
    }

    private GameSession newSession(Color humanColor) {
        GamePreferences preferences = new GamePreferences(
                PlayerColor.of(humanColor), Difficulty.EASY, null, null,
                CommentaryStyle.NEUTRAL, "qwen2.5:7b-instruct");
        return new GameSession(GameState.newGame(humanColor), preferences,
                AiSettings.of(AiPreset.EASY), clock.instant());
    }

    /** Часы, которые тест двигает вручную (для проверки TTL без ожидания). */
    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
