package com.example.backend.config;

import com.example.backend.game.ai.AlphaBetaSearch;
import com.example.backend.game.ai.PositionEvaluator;
import com.example.backend.repository.LlmCommentRepository;
import com.example.backend.service.CommentProvider;
import com.example.backend.service.CommentTextSanitizer;
import com.example.backend.service.FallbackCommentProvider;
import com.example.backend.service.LlmCommentProvider;
import com.example.backend.service.LlmServiceClient;
import com.example.backend.session.GameRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.random.RandomGenerator;

/**
 * Сборка живого слоя игры: часы (для TTL/таймстампов), реестр партий (ТЗ 4.2),
 * поиск ИИ (ТЗ 6), клиент LLM-сервиса и провайдер комментариев (ТЗ 7, Sprint 4).
 * Реестр и поиск — чистые классы без Spring; Spring владеет их жизненным циклом
 * через эти bean-методы.
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

    /** Клиент к llm-service (прокси к Ollama) с таймаутом из настроек. */
    @Bean
    public LlmServiceClient llmServiceClient(LlmProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.timeoutMs());
        factory.setReadTimeout((int) properties.timeoutMs());
        RestClient restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.baseUrl())
                .build();
        return new LlmServiceClient(restClient);
    }

    /** Пост-фильтр ответа LLM: русский язык, чистка разметки, ограничение длины. */
    @Bean
    public CommentTextSanitizer commentTextSanitizer() {
        return new CommentTextSanitizer();
    }

    /** Fallback-комментарий: сохранённые реплики LLM (B3) или детерминированный текст. */
    @Bean
    public FallbackCommentProvider fallbackCommentProvider(LlmCommentRepository repository,
                                                           CommentTextSanitizer sanitizer) {
        return new FallbackCommentProvider(repository, sanitizer, RandomGenerator.getDefault());
    }

    /**
     * Провайдер комментариев к ходу ИИ: живой LLM через llm-service с fallback.
     * Является основным бином {@link CommentProvider} (ссылка через тип).
     */
    @Bean
    public CommentProvider commentProvider(LlmServiceClient llmClient,
                                           LlmProperties properties,
                                           FallbackCommentProvider fallback,
                                           CommentTextSanitizer sanitizer) {
        return new LlmCommentProvider(llmClient, properties, fallback, sanitizer);
    }
}
