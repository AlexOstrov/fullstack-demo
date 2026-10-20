# Dockerfile для game-service (Sprint 7)
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Копируем файлы сборки Maven
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./

# Загружаем зависимости (кешируемый слой)
RUN ./mvnw dependency:go-offline -B -q

# Копируем исходный код
COPY src/ src/

# Собираем приложение
RUN ./mvnw package -DskipTests -B -q

# Финальный образ runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Копируем собранный jar
COPY --from=builder /app/target/game-service-*.jar app.jar

# Создаём пользователя без привилегий
RUN addgroup -S appuser && adduser -S appuser -G appuser
USER appuser

# Порт приложения
EXPOSE 8080

# Переменные окружения с valores по умолчанию
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/app
ENV SPRING_DATASOURCE_USERNAME=app
ENV SPRING_DATASOURCE_PASSWORD=app
ENV GAME_TTL_MINUTES=60
ENV GAME_CLEANUP_INTERVAL_MS=300000
ENV AI_TT_CAPACITY=65536
ENV LLM_ENABLED=true
ENV LLM_BASE_URL=http://llm-service:8081
ENV LLM_MODEL=qwen2.5:7b-instruct

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
