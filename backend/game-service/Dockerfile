# Dockerfile для game-service (Sprint 7)
# Контекст сборки: ./backend (корень Maven-агрегатора backend/pom.xml)
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. POM агрегатора
COPY pom.xml ./

# 2. POM обоих модулей: агрегатор читает их все, даже если собираем один
COPY game-service/pom.xml game-service/pom.xml
COPY llm-service/pom.xml llm-service/pom.xml

# 3. Исходники только целевого модуля
COPY game-service/src game-service/src

# 4. Сборка целевого модуля. Кэш ~/.m2 — между пересборками (BuildKit)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl game-service -am package -DskipTests \
    && cp game-service/target/game-service-*.jar /build/app.jar

# --- Runtime ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appuser && adduser -S appuser -G appuser
COPY --from=builder /build/app.jar /app/app.jar
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/app
ENV SPRING_DATASOURCE_USERNAME=app
ENV SPRING_DATASOURCE_PASSWORD=app
ENV GAME_TTL_MINUTES=60
ENV GAME_CLEANUP_INTERVAL_MS=300000
ENV AI_TT_CAPACITY=65536
ENV LLM_ENABLED=true
ENV OLLAMA_BASE_URL=http://llm-service:8081
ENV LLM_MODEL=qwen2.5:7b-instruct

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
