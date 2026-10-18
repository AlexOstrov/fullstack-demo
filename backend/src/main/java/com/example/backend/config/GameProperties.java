package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Настройки живого реестра партий (ТЗ 4.2). Env: {@code GAME_TTL_MINUTES},
 * {@code GAME_CLEANUP_INTERVAL_MS}.
 */
@ConfigurationProperties(prefix = "game")
public record GameProperties(long ttlMinutes, long cleanupIntervalMs) {

    public GameProperties {
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("game.ttl-minutes должен быть положительным");
        }
        if (cleanupIntervalMs <= 0) {
            throw new IllegalArgumentException("game.cleanup-interval-ms должен быть положительным");
        }
    }

    /** TTL неактивной партии как {@link Duration}. */
    public Duration ttl() {
        return Duration.ofMinutes(ttlMinutes);
    }
}
