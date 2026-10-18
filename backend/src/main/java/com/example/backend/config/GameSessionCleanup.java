package com.example.backend.config;

import com.example.backend.session.GameRegistry;
import com.example.backend.session.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Фоновое вытеснение неактивных партий по TTL (ТЗ 4.2).
 * Интервал — {@code GAME_CLEANUP_INTERVAL_MS}, TTL — {@code GAME_TTL_MINUTES}.
 */
@Component
public class GameSessionCleanup {

    private static final Logger log = LoggerFactory.getLogger(GameSessionCleanup.class);

    private final GameRegistry registry;

    public GameSessionCleanup(GameRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${game.cleanup-interval-ms}")
    public void evictExpiredSessions() {
        List<GameSession> expired = registry.evictExpired();
        if (!expired.isEmpty()) {
            log.info("TTL-вытеснение: удалено партий — {} (осталось {})",
                    expired.size(), registry.size());
        }
    }
}
