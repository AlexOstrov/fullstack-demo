# Runbook

## База данных (PostgreSQL 16, Docker)

```powershell
cd D:\projects\fullstack-demo
docker compose up -d db          # поднять только БД
docker compose ps                # статус/healthcheck
```

Подключение: `jdbc:postgresql://localhost:5432/app`, пользователь `app`, пароль `app`.
Схема применяется автоматически Flyway при старте backend (`backend/src/main/resources/db/migration/`).
Проверить таблицы:

```powershell
docker exec -it fullstack-db psql -U app -d app -c "\dt"
docker exec -it fullstack-db psql -U app -d app -c "select * from tournament order by id desc limit 5;"
```

## Ollama (модель-комментатор)

```powershell
ollama serve
ollama pull qwen2.5:7b-instruct   # или qwen2.5:3b-instruct для скорости
ollama ps                          # проверить, что модель загружена
```

Выгрузить модель из памяти при необходимости:

```powershell
ollama stop qwen2.5:7b-instruct
```

## Backend (Spring Boot 4.1.1, порт 8080)

```powershell
cd backend
./mvnw spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx1536m -XX:MaxMetaspaceSize=512m"
```

## Frontend (Vite, порт 5173)

```powershell
cd frontend
npm run dev
```

## Docker (db + backend + frontend + llm-service)

```powershell
cd D:\projects\fullstack-demo
docker compose up -d
```

## Тесты

```powershell
cd backend ; ./mvnw test
cd frontend ; npm test
```

## Переменные окружения

- **БД:** `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- **LLM:** `OLLAMA_BASE_URL`, `LLM_MODEL`, `LLM_ALLOWED_MODELS`, `LLM_TIMEOUT_MS`, `LLM_MAX_RETRIES`, `LLM_TEMPERATURE`, `LLM_NUM_CTX`, `LLM_ENABLED`, `OLLAMA_KEEP_ALIVE`.
- **Игра:** `AI_PRESET_*`, `GAME_TTL_MINUTES`.
