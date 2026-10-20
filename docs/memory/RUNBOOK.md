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
docker exec -it fullstack-db psql -U app -d app -c "select * from llm_comments order by id desc limit 5;"
```

Миграции: `V1` (демо `items`, затем удалена), `V2__drop_items`, `V3__create_tournament`, `V4__create_llm_comments`.
Живые партии в БД не хранятся (ADR-0001) — только результаты завершённых (`tournament`) и избранные комментарии (`llm_comments`, запись — бэклог B2).

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

## Backend — game-service (API партии, порт 8080)

```powershell
cd backend
./mvnw -pl game-service spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx1536m -XX:MaxMetaspaceSize=512m"
```

## Backend — llm-service (прокси к Ollama, порт 8081)

```powershell
cd backend
./mvnw -pl llm-service spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx1024m -XX:MaxMetaspaceSize=256m"
```

Переменные окружения `llm-service`:
- `OLLAMA_BASE_URL` (по умолчанию `http://localhost:11434`)
- `LLM_MODEL` (по умолчанию `qwen2.5:7b-instruct`)
- `LLM_ALLOWED_MODELS` (по умолчанию `qwen2.5:7b-instruct,qwen2.5:14b-instruct,llama3.1:8b-instruct`)
- `LLM_REFRESH_INTERVAL_MS` (по умолчанию `300000`)

Эндпоинты `llm-service` (базовый путь `/api`):
- `GET /api/models` — список моделей (Ollama `/api/tags` ∩ `LLM_ALLOWED_MODELS`)
- `POST /api/chat` (TEXT_EVENT_STREAM) — streaming чата к Ollama (`{"prompt": "...", "model": "..."}`)

Альтернатива — jar (для быстрых smoke-проверок):

```powershell
cd backend
./mvnw -DskipTests package
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

Smoke-проверка (API партии, ТЗ 5) и остановка:

```powershell
(Invoke-WebRequest http://localhost:8080/api/config/llm-models -UseBasicParsing).Content       # список моделей LLM

$start = Invoke-RestMethod -Method Post http://localhost:8080/api/game/start `
  -ContentType 'application/json' `
  -Body '{"playerColor":"WHITE","difficulty":"EASY","commentaryStyle":"FRIENDLY_MASTER","llmModel":"qwen2.5:7b-instruct"}'
$start.gameId
Invoke-RestMethod http://localhost:8080/api/game/$($start.gameId)                              # состояние: 7 допустимых ходов белых

Get-NetTCPConnection -LocalPort 8080 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess }
```

## API партии (ТЗ 5)

Базовый путь `/api`. Ошибки — единый формат `{timestamp,status,code,message,path}` (ТЗ 5.3).

| Метод | Путь | Назначение |
|-------|------|------------|
| POST | `/api/game/start` | Создать партию (`playerColor`, `difficulty`, `aiDepth`/`aiTimeBudgetMs` — для `CUSTOM`, `commentaryStyle`, `llmModel` из allowlist) → `201` |
| GET | `/api/game/{id}` | Текущее состояние (`board`, `legalMoves`, `forcedCapture`, `status`, …) |
| GET | `/api/game/{id}/moves` | История ходов и комментариев |
| POST | `/api/game/{id}/move` | Ход игрока: `{path:[{row,col}...], clientMoveNumber}`; `clientMoveNumber ≠ moveNumber` → `STALE_STATE` |
| POST | `/api/game/{id}/ai-move` | Ход ИИ (только когда очередь ИИ) + комментарий; повторный запрос во время счёта → `AI_BUSY` |
| POST | `/api/game/{id}/resign` | Сдаться |
| POST | `/api/game/{id}/draw` | Предложить ничью (решает ИИ) → `{accepted, game}` |
| POST | `/api/game/{id}/restart` | Новая партия (новый `gameId`); без тела — с прежними настройками |
| GET | `/api/config/llm-models` | Список моделей LLM для UI |

Коды ошибок: `GAME_NOT_FOUND` (404), `AI_BUSY`/`STALE_STATE` (409), `NOT_YOUR_TURN`/`INVALID_MOVE`/`GAME_FINISHED`/`VALIDATION_ERROR` (400), `INTERNAL_ERROR` (500).
Результат завершённой партии сохраняется в `tournament` автоматически (best-effort, ТЗ 4.7).

## Frontend (Vite, порт 5173)

```powershell
cd frontend
npm run dev
```

Smoke-проверка (вторая строка проверяет прокси `/api` на backend):

```powershell
(Invoke-WebRequest http://localhost:5173/ -UseBasicParsing).StatusCode                             # 200
(Invoke-WebRequest http://localhost:5173/api/config/llm-models -UseBasicParsing).StatusCode        # 200 при запущенном backend
Get-NetTCPConnection -LocalPort 5173 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess }
```

## Docker (все сервисы)

```powershell
cd D:\projects\fullstack-demo
docker compose up -d              # поднять все сервисы (db, game-service, llm-service, frontend)
docker compose ps                 # статус/healthcheck
docker compose logs -f            # логи всех сервисов
docker compose down               # остановить все сервисы
```

Доступ после запуска:
- Frontend: http://localhost:80
- Backend (game-service): http://localhost:8080
- LLM-сервис (llm-service): http://localhost:8081
- PostgreSQL: localhost:5432 (пользователь `app`, пароль `app`)

Запуск только БД (без остальных сервисов):

```powershell
docker compose up -d db
docker compose ps
docker compose logs -f db
```

## Тесты и линтеры

```powershell
# backend: JUnit 5 — требует запущенного db (docker compose up -d db)
cd backend ; ./mvnw test

# frontend: Vitest + RTL
cd frontend ; npm test          # однократно
cd frontend ; npm run test:watch

# линт и сборка
cd frontend ; npm run lint      # oxlint
cd frontend ; npm run build     # tsc -b && vite build (типизирует и тесты)
```

## Push (git → GitHub)

```powershell
git push origin main
```

Аутентификация идёт **по PAT из `.secrets`** через локальный credential helper (записан в `.git/config` репозитория, см. ADR-0010):

```powershell
# посмотреть helper'ы с указанием источника (нужны: пустая строка-сброс и !f()-скрипт из .git/config)
git config --show-origin --get-all credential.helper

# проверить выдачу кредов (пароль не показывать в логи!)
$out = "protocol=https`nhost=github.com`n`n" | git credential fill
($out | Select-String '^username=').Line
```

- Системный Git Credential Manager (`manager`) сброшен пустым значением helper в local-конфиге — в цепочке остаётся только скрипт из `.secrets`.
- Файл `.secrets*` в git не попадает (`.gitignore`).
- Примечание: helper записывать через `git config --local --add credential.helper '!f() ...'` — прямая запись строки в `.git/config` обрезается git-парсером на первой `;`.

## Переменные окружения

- **БД:** `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- **Игра:** `GAME_TTL_MINUTES` (по умолчанию `60`), `GAME_CLEANUP_INTERVAL_MS` (`300000`) — TTL и период фонового вытеснения партий.
- **ИИ:** `AI_TT_CAPACITY` (`65536`); пресеты — `AI_PRESET_EASY_DEPTH`/`AI_PRESET_EASY_TIME_BUDGET_MS`, `AI_PRESET_MEDIUM_*` (4/1500), `AI_PRESET_HARD_*` (6/3000). `CUSTOM` — глубина/бюджет из запроса (ТЗ 6.1).
- **LLM (game-service — подключение к llm-service):** `LLM_ENABLED` (по умолчанию `true` — живой LLM через `llm-service`; `false` — только fallback), `LLM_TIMEOUT_MS` (по умолчанию `8000`), `OLLAMA_BASE_URL` (адрес llm-service, по умолчанию `http://localhost:8081`).
- **LLM (llm-service — прокси к Ollama):** `OLLAMA_BASE_URL` (по умолчанию `http://localhost:11434`), `LLM_MODEL` (по умолчанию `qwen2.5:7b-instruct`), `LLM_ALLOWED_MODELS`, `LLM_REFRESH_INTERVAL_MS` (конфиг `llm.ollama.*` / `app.llm.*` в `backend/llm-service/src/main/resources/application.yml`).
- Значения по умолчанию — в `backend/src/main/resources/application.yml`; переопределение — только env (секретов в репозитории нет).
