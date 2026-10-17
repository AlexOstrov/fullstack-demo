# Проект: fullstack-demo

## Стек

- Backend: Java 21, Spring Boot, Maven
- Frontend: React, TypeScript, Vite
- Database: PostgreSQL 16
- Migration: Flyway
- API base path: /api
- Backend port: 8080
- Frontend dev port: 5173

## Команды

### Backend

Запуск:

```bash
cd backend
./mvnw spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx1536m -XX:MaxMetaspaceSize=512m"
```

### frontend

Запуск:

```bash
cd frontend
npm run dev
```

## Правила генерации кода

- Использовать только актуальный Spring Boot 3.x и `jakarta.*`, не `javax.*`.
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
