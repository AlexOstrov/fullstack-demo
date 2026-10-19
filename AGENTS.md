# Проект: fullstack-demo

## Стек

- Backend: Java 21, Spring Boot, Maven
- Frontend: React, TypeScript, Vite
- Database: PostgreSQL 16
- Migration: Flyway
- API base path: /api
- Backend port: 8080
- Frontend dev port: 5173

## Артефактная память (обязательно)

При старте новой сессии **сначала прочитай `docs/memory/STATE.md`** и продолжай с «Следующая задача» — не анализируй весь проект заново. Бэклог — в `docs/memory/BACKLOG.md`.

После каждой реализованной фичи (это часть Definition of Done):
1. Обнови `docs/memory/STATE.md` (текущий спринт/задача, последняя фича, следующая задача, блокеры).
2. Добавь строку в `docs/memory/FEATURES.md` (ID, спринт, файлы, тесты/DoD, коммит).
3. При принятии решения — запись в `docs/memory/DECISIONS.md`.
4. При изменении запуска — обнови `docs/memory/RUNBOOK.md`.
5. Актуализируй статусы в `docs/memory/BACKLOG.md`.

Процесс описан в ТЗ `game-chashki-TZ.md`, раздел 11.

## Команды

### Backend (multi-module: `game-service` + `llm-service`)

Запуск `game-service` (API партии, порт 8080):

```bash
cd backend
./mvnw -pl game-service spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx1536m -XX:MaxMetaspaceSize=512m"
```

Запуск `llm-service` (прокси к Ollama, порт 8081):

```bash
cd backend
./mvnw -pl llm-service spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx1024m -XX:MaxMetaspaceSize=256m"
```

Сборка всех модулей:

```bash
cd backend
./mvnw clean install
```

### frontend

Запуск:

```bash
cd frontend
npm run dev
```

## Правила генерации кода

- Использовать актуальный Spring Boot 4.x (в проекте — 4.1.1) и `jakarta.*`, не `javax.*`.
- Не использовать Lombok.
- DTO реализовывать как Java records.
- Для запросов использовать Jakarta Validation.
- Изменения схемы БД делать только через новые файлы Flyway.
- Не редактировать уже примененные миграции.
- Не хранить секреты в репозитории.
- Для локальной разработки использовать пользователя БД `app` и пароль `app`.
- Отвечать на русском языке.
- Если правится несколько файлов, указывать путь к каждому файлу.

## Полный порядок запуска проекта

Обычный порядок:

1. Запустить PostgreSQL (docker).
2. Запустить backend.
3. Запустить frontend.
4. Запустить/использовать OpenCode с локальной моделью.

### Docker PostgreSQL

Проверьте службу и подключитесь к базе.

```powershell
cd C:\dev\fullstack-demo
docker compose up -d
```

## Commit messages
Use Conventional Commits format in Russian:
- feat: new feature
- fix: bug fix
- docs: documentation changes
- refactor: code refactoring
- test: adding tests
- chore: maintenance

Example: `feat(api): add pagination support for GET /api/items`
