package com.example.backend.config;

import com.example.backend.game.ai.AlphaBetaSearch;
import com.example.backend.game.ai.PositionEvaluator;
import com.example.backend.session.GameRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Сборка живого слоя игры: часы (для TTL/таймстампов), реестр партий (ТЗ 4.2),
 * поиск ИИ (ТЗ 6). Реестр и поиск — чистые классы без Spring; Spring владеет
 * их жизненным циклом через эти bean-методы.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({GameProperties.class, AiProperties.class, LlmProperties.class})
public class GameConfiguration {

    /** Часы процесса: TTL партий и таймстампы (инъекция Clock — для тестов с управляемым временем). */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Реестр партий с TTL неактивных сессий (ТЗ 4.2). */
    @Bean
    public GameRegistry gameRegistry(GameProperties properties, Clock clock) {
        return new GameRegistry(properties.ttl(), clock);
    }

    /** Минимакс с альфа-бета — потокобезопасен: TT создаётся на каждый поиск (ТЗ 6). */
    @Bean
    public AlphaBetaSearch alphaBetaSearch() {
        return new AlphaBetaSearch();
    }

    /** Статическая оценка позиции — используется и поиском, и решением ИИ о ничьей (ТЗ 5, 6). */
    @Bean
    public PositionEvaluator positionEvaluator() {
        return new PositionEvaluator();
    }
}
