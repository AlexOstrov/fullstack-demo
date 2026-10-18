# Состояние разработки — «Русские шашки»

> Точка возобновления. **Читать первым** при старте новой сессии, затем продолжать с «Следующая задача».

## Сейчас

- ТЗ: **финализировано (v2.4)** — `game-chashki-TZ.md`
- Спринт: **Sprint 0 — Инициализация (переиспользование каркаса)**, не начат
- В работе: —
- Следующая задача: **S0.1 — backend: убрать дубли-файлы вне `src`, слить `application.properties` в `application.yml`, подтвердить запуск с БД**

## Задачи Sprint 0

- S0.1 — backend: чистка дублей вне `src`, слияние конфигов, запуск + подключение к `db`.
- S0.2 — frontend: убрать демо-`items`, добавить заглушку доски.
- S0.3 — `docker compose up -d db`, проверка Flyway на `V1__create_items.sql`.
- S0.4 — актуализировать `docs/memory/{STATE,FEATURES,DECISIONS,RUNBOOK,BACKLOG}.md`, связать с `AGENTS.md`.
- S0.5 — тесты/линтеры (JUnit, Vitest/RTL), Conventional Commits.

## Последняя завершённая фича

- нет

## Блокеры

- нет

## Ссылки (ключевые файлы)

- ТЗ: `game-chashki-TZ.md`
- Правила проекта: `AGENTS.md`
- Бэклог: `docs/memory/BACKLOG.md`
- Журнал фич: `docs/memory/FEATURES.md`
- Решения: `docs/memory/DECISIONS.md`
- Запуск: `docs/memory/RUNBOOK.md`
