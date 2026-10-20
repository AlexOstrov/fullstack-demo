# Dockerfile для llm-service (Sprint 7)
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Копируем файлы сборки Maven
COPY ../.mvn/ .mvn/
COPY ../mvnw ../mvnw.cmd ../pom.xml ./
COPY pom.xml ./

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
COPY --from=builder /app/target/llm-service-*.jar app.jar

# Создаём пользователя без привилегий
RUN addgroup -S appuser && adduser -S appuser -G appuser
USER appuser

# Порт приложения
EXPOSE 8081

# Переменные окружения с valores по умолчанию
ENV OLLAMA_BASE_URL=http://host.docker.internal:11434
ENV LLM_MODEL=qwen2.5:7b-instruct
ENV LLM_ALLOWED_MODELS=qwen2.5:7b-instruct,qwen2.5:14b-instruct,llama3.1:8b-instruct
ENV LLM_REFRESH_INTERVAL_MS=300000

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
