# Состояние разработки — «Русские шашки»

> Точка возобновления. **Читать первым** при старте новой сессии, затем продолжать с «Следующая задача».

## Сейчас

- ТЗ: **финализировано (v2.4)** — `game-chashki-TZ.md`
- Спринт: **Sprint 1 — Ядро игры (rules engine)**, в работе
- В работе: —
- Следующая задача: **S1.2 — единый генератор ходов (ТЗ 4.6): простые (в т.ч. бой назад), дамки, серийные взятия**

## Задачи Sprint 1 (ТЗ, раздел 14)

- S1.1 ✅ — модель `Board`, `Piece`, `GameState` (ТЗ 4.1). (коммит `501e6fa`)
- S1.2 — единый генератор ходов (ТЗ 4.6): простые (в т.ч. бой назад), дамки, серийные взятия.
- S1.3 — правило большинства; превращение, включая превращение в бою (ТЗ 3.4–3.5).
- S1.4 — валидация пути и применение хода; снятие шашек по мере прыжка (ТЗ 4.4–4.5).
- S1.5 — определение конца партии: нет ходов, троекратное повторение, 15 ходов дамками (ТЗ 3.6).
- S1.6 — unit-тесты (ТЗ 12) — полное покрытие спецправил, включая «турецкий удар».

## Задачи Sprint 0 (завершён)

- S0.1 ✅ — backend: чистка дублей вне `src`, слияние конфигов, запуск + подключение к `db`. (коммит `2d6ce68`)
- S0.2 ✅ — frontend: убрать демо-`items`, добавить заглушку доски. (коммит `b9c262f`)
- S0.3 ✅ — `docker compose up -d db`, проверка Flyway на `V1__create_items.sql`. (без кодовых изменений — верификация, см. FEATURES.md)
- S0.4 ✅ — актуализировать `docs/memory/{STATE,FEATURES,DECISIONS,RUNBOOK,BACKLOG}.md`, связать с `AGENTS.md`. (коммит `1632b17` + этот docs-коммит)
- S0.5 ✅ — тесты/линтеры (JUnit, Vitest/RTL), Conventional Commits. (коммит `5b935f8`)

## Последняя завершённая фича

- **F-001 (S1.1)** — модель игрового ядра `com.example.backend.game`: `Color`, `Piece` (EMPTY/шашки/дамки), `Board` 8×8 (начальная позиция ТЗ 3.1, `positionKey` для повторений), `Cell`, `Move` (путь + тип), `MoveType`, `GameStatus`, `LlmComment`, `GameState` (все поля ТЗ 4.1); тесты `./mvnw test` — 24/24 (коммит `501e6fa`).
- **S0.5** — тесты/линтеры: frontend — Vitest 5 + RTL + jsdom (`vitest.config.ts`, `src/test/setup.ts`, скрипт `npm test`), 8 тестов в `App.test.tsx`/`Board.test.tsx` зелёные, `npm run lint` + `npm run build` OK; backend — `./mvnw test`: 3/3 (в т.ч. новый `ItemDtoTest`), `BUILD SUCCESS`; smoke: frontend 5173 → 200, прокси `/api/items` → 200 (коммит `5b935f8`).
- **S0.4** — память: `DECISIONS.md` пополнен ADR-0006..0008, `RUNBOOK.md` приведён к факту (compose пока только `db`, smoke-проверки/остановка, тесты и линт), `BACKLOG.md` проверен (B1–B3 `Idea` — актуально), связка с `AGENTS.md` проверена (коммит `1632b17`).
- **S0.3** — проверка БД/Flyway: `docker compose up -d db` → `fullstack-db` healthy; штатный запуск против основной БД — `validated 1 migration`, `Schema is up to date`, `GET /api/items` → 200; свежая миграция на чистой БД (временный контейнер `flyway-check`, порт 5433, удалён) — `Empty Schema → v1`, `flyway_schema_history.success = t`, `POST /api/items` → 201. Код не менялся.
- **S0.2** — frontend-заглушка: `App.tsx` переписан (демо-`items` и `axios` убраны), добавлен `src/components/Board.tsx` — доска 8×8 (CSS Grid, начальная позиция, белые снизу), стили в `App.css`, title/lang в `index.html`; `npm run lint` и `npm run build` — OK (коммит `b9c262f`).
- **S0.1** — backend-очистка: удалены дубли вне `src` (`config/CordConfig.java`, `controller/`, `dto/`, `repository/`), `application.properties` слит в `application.yml`; запуск подтверждён: Flyway `V1__create_items.sql` применён, `GET/POST /api/items` → HTTP 200/201 (коммит `2d6ce68`).

## Блокеры

- нет

## Ссылки (ключевые файлы)

- ТЗ: `game-chashki-TZ.md`
- Правила проекта: `AGENTS.md`
- Бэклог: `docs/memory/BACKLOG.md`
- Журнал фич: `docs/memory/FEATURES.md`
- Решения: `docs/memory/DECISIONS.md`
- Запуск: `docs/memory/RUNBOOK.md`
