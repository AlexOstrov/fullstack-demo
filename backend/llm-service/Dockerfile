# Dockerfile для llm-service (Sprint 7)
# Контекст сборки: ./backend (корень Maven-агрегатора backend/pom.xml)
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. POM агрегатора
COPY pom.xml ./

# 2. POM обоих модулей: агрегатор читает их все, даже если собираем один
COPY game-service/pom.xml game-service/pom.xml
COPY llm-service/pom.xml llm-service/pom.xml

# 3. Исходники только целевого модуля
COPY llm-service/src llm-service/src

# 4. Сборка целевого модуля. Кэш ~/.m2 — между пересборками (BuildKit)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl llm-service -am package -DskipTests \
    && cp llm-service/target/llm-service-*.jar /build/app.jar

# --- Runtime ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appuser && adduser -S appuser -G appuser
COPY --from=builder /build/app.jar /app/app.jar
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8081

ENV OLLAMA_BASE_URL=http://host.docker.internal:11434
ENV LLM_MODEL=qwen2.5:7b-instruct
ENV LLM_ALLOWED_MODELS=qwen2.5:7b-instruct,qwen2.5:14b-instruct,llama3.1:8b-instruct
ENV LLM_REFRESH_INTERVAL_MS=300000

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
