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

Альтернатива — jar (для быстрых smoke-проверок):

```powershell
cd backend
./mvnw -DskipTests package
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

Smoke-проверка и остановка:

```powershell
(Invoke-WebRequest http://localhost:8080/api/items -UseBasicParsing).StatusCode   # 200
Get-NetTCPConnection -LocalPort 8080 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess }
```

## Frontend (Vite, порт 5173)

```powershell
cd frontend
npm run dev
```

Smoke-проверка (вторая строка проверяет прокси `/api` на backend):

```powershell
(Invoke-WebRequest http://localhost:5173/ -UseBasicParsing).StatusCode           # 200
(Invoke-WebRequest http://localhost:5173/api/items -UseBasicParsing).StatusCode  # 200 при запущенном backend
Get-NetTCPConnection -LocalPort 5173 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess }
```

## Docker

Сейчас в `docker-compose.yml` есть **только сервис `db`** (остальные — в Sprint 7):

```powershell
cd D:\projects\fullstack-demo
docker compose up -d db          # поднять БД
docker compose ps                # статус/healthcheck
docker compose logs -d db        # логи
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
- **LLM:** `OLLAMA_BASE_URL`, `LLM_MODEL`, `LLM_ALLOWED_MODELS`, `LLM_TIMEOUT_MS`, `LLM_MAX_RETRIES`, `LLM_TEMPERATURE`, `LLM_NUM_CTX`, `LLM_ENABLED`, `OLLAMA_KEEP_ALIVE`.
- **Игра:** `AI_PRESET_*`, `GAME_TTL_MINUTES`.
