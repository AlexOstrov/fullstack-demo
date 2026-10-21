# Состояние разработки — «Русские шашки»

> Точка возобновления. **Читать первым** при старте новой сессии, затем продолжать с «Следующая задача».

## Сейчас

- ТЗ: **финализировано (v2.4)** — `game-chashki-TZ.md`
- Спринт: **Sprint 3 — API, in-memory реестр и персистентность**, ✅ **завершён** (S3.1–S3.6); **Sprint 4 — LLM-прокси и интеграция** — завершён (S4.1–S4.5); **Sprint 5 — Frontend: игровой цикл** — завершён (S5.1–S5.6); **Sprint 6 — Анимации и визуальная подача** — завершён (S6.1–S6.2); **Sprint 7 — Docker и интеграция** — завершён (S7.1–S7.3); **Sprint 8 — Стабилизация и приёмка** — завершён (S8.1–S8.4); **Sprint 9 — Стабилизация Docker-запуска** — завершён (S9.1–S9.4); **Sprint 10 — UI/UX-редизайн** — завершён (S10, F-025)
- Ревью Sprint 0–2: ✅ **проведено** — `docs/memory/REVIEW-sprint-0-2.md`; R-1 исправлено (F-012); R-3/R-4 закрыты в S3 (env-настройки `AI_PRESET_*`, маппинг `CUSTOM`); R-2/R-5/R-6/R-7 → бэклог **B4** (R-5 закрыт в S4.3)
- В работе: —
- Следующая задача: — (спринты 0–10 завершены; открытые идеи — B1, B3, B5, остаток B4 в `BACKLOG.md`)

## Задачи Sprint 1 (ТЗ, раздел 14) — все выполнены

- S1.1 ✅ — модель `Board`, `Piece`, `GameState` (ТЗ 4.1). (коммит `501e6fa`)
- S1.2 ✅ — единый генератор ходов (ТЗ 4.6): простые (в т.ч. бой назад), дамки, серийные взятия. (коммит `d7fe4b3`)
- S1.3 ✅ — правило большинства; превращение, включая превращение в бою (ТЗ 3.4–3.5). (коммит `f638813`)
- S1.4 ✅ — валидация пути и применение хода; снятие шашек по мере прыжка (ТЗ 4.4–4.5). (коммит `c0ae6c6`)
- S1.5 ✅ — определение конца партии: нет ходов, троекратное повторение, 15 ходов дамками (ТЗ 3.6). (коммит `7d3f10f`)
- S1.6 ✅ — unit-тесты (ТЗ 12): полное покрытие спецправил, включая «турецкий удар»; сдача/ничья по согласию в движке (ТЗ 3.6). (коммит `033bdc0`)

## Задачи Sprint 2 (ТЗ, раздел 14)

- S2.1 ✅ — оценка позиции (ТЗ 6): `PositionEvaluator` — материал, продвижение, безопасность, мобильность, центр (дамки), темп. (коммит `4d0ae31`)
- S2.2 ✅ — альфа-бета + итеративное углубление + сортировка ходов (ТЗ 6): `AlphaBetaSearch`, `MoveOrdering`, `AiSettings`/`AiPreset`, общий `MoveApplier`. (коммит `e36fe33`)
- S2.3 ✅ — транспозиционная таблица на Zobrist; ограничение размера (ТЗ 6): `Zobrist`, `TranspositionTable`, ветвление в `AlphaBetaSearch`. (коммит `209806b`)
- S2.4 ✅ — обрезка по повторению/ничьей (ТЗ 6): троекратность и «30 полуходов дамками» = 0 внутри поиска. (коммит `d0bec33`)
- S2.5 ✅ — тесты на эталонных позициях иベンчмарк бюджета времени (ТЗ 12): `AiReferencePositionsTest`, DoD спринта выполнено. (коммит `33b64c1`)

## Задачи Sprint 3 (ТЗ, раздел 14) — все выполнены

- S3.1 ✅ — реестр партий: `ConcurrentHashMap` + per-game lock + TTL (ТЗ 4.2): `session/GameSession`, `session/GameRegistry`, фоновое вытеснение. (коммит `82a4b90`)
- S3.2 ✅ — эндпоинты: start, get, moves, move, ai-move, resign, draw, restart, `GET /api/config/llm-models` (ТЗ 5). (коммит `0a7f031`)
- S3.3 ✅ — DTO-`record` + Jakarta Validation; единый формат ошибок (`error/*`); allowlist `llmModel`. (коммит `0a7f031`)
- S3.4 ✅ — идемпотентность (`clientMoveNumber`), `AI_BUSY` (в т.ч. при конкурентном запросе), `STALE_STATE` (ТЗ 5.4). (коммит `0a7f031`)
- S3.5 ✅ — персистентность: Flyway `V2__drop_items`/`V3__create_tournament`/`V4__create_llm_comments`, JPA, автосохранение результата best-effort (ТЗ 4.7). (коммит `2e02bf9`)
- S3.6 ✅ — интеграционные тесты API, гонок и персистентности (ТЗ 12): `controller/GameApiIntegrationTest` (10), `service/GameServiceTest` (16). (коммит `0a7f031`)

## Задачи Sprint 4 (ТЗ, раздел 14) — LLM-прокси

- S4.1 ✅ — микросервис-прокси `llm-service` к Ollama (ТЗ 7): порт 8081, `LLM_MODEL` = `qwen2.5:7b-instruct` по умолчанию, `GET /api/models` = Ollama `/api/tags` ∩ `LLM_ALLOWED_MODELS`, `POST /api/chat` (streaming NDJSON), `OllamaClient` (WebClient, таймауты), `ModelCatalog` (кэш + `@PostConstruct`), `LlmChatClient` (проверка allowlist), тесты 13/13. (коммит S4.1)
- S4.2 ✅ — интеграция `game-service` с `llm-service` (ТЗ 7): `LlmServiceClient` (RestClient к `llm-service:8081/api/chat`, SSE-парсинг), `LlmCommentProvider` (живой LLM + детерминированный fallback при сбое/таймауте/`LLM_ENABLED=false`), `CommentProvider` — основной бин через `GameConfiguration`, `LlmComment` с `fallback=false` и моделью при успешном ответе. Тесты: `LlmServiceClientTest` (6), `LlmCommentProviderTest` (5); `./mvnw test -pl game-service` — **172/172**.
- S4.3 ✅ — комментарии с учётом стиля и эмоции (ТЗ 7, R-5 из B4): `Emotion` enum (`NEUTRAL`, `EXCITED`, `CONFIDENT`, `CAUTIOUS`) в `dto.game`; `LlmComment` расширен полями `style` (`CommentaryStyle`) и `emotion` (`Emotion`); `LlmCommentProvider` и `FallbackCommentProvider` передают `style`/`emotion` из `session.preferences()`; маппинг стиля на эмоцию: FRIENDLY_MASTER→NEUTRAL, COMPETITIVE→CONFIDENT, NEUTRAL→NEUTRAL. Тесты: `LlmCommentProviderTest` (6, вкл. `emotionOfMapsStyleToEmotion`); `./mvnw test -pl game-service` — **173/173**; весь multi-module — **186/186**.
- S4.4 ✅ — сохранение комментариев LLM в `llm_comments` (ТЗ 7, B2): `LlmCommentEntity` (JPA для таблицы `llm_comments`), `LlmCommentRepository`, `LlmCommentRecorder` (best-effort, как `TournamentRecorder`); интеграция в `GameService.aiMove()` — комментарий сохраняется после хода ИИ; миграция `V5__add_game_id_to_llm_comments.sql` добавляет колонку `game_id`. Тесты: `LlmCommentRecorderTest` (4), `GameServiceTest` (обновлён, проверяет `llmCommentRepository.save`); `./mvnw test -pl game-service` — **177/177**; весь multi-module — **190/190**.
- S4.5 ✅ — лайки комментариев LLM (ТЗ 7, B2): `LikedRequest` DTO (+ Jakarta Validation), `LlmCommentService` (`toggleLike` — вверх сохраняет в БД, вниз удаляет из БД), `LlmCommentController` (`POST /api/game/{id}/comments/{commentId}/like` → 204 No Content / 404 Not Found), `LlmCommentEntity.liked` + миграция `V6__add_liked_to_llm_comments.sql`; `LlmComment.liked`, `AiTurnDto.liked`, `CommentDto.liked` в ответах API. Тесты: `LlmCommentServiceTest` (4), `LlmCommentControllerTest` (3), `GameServiceTest` (+liked), `LlmCommentProviderTest` (+liked); `./mvnw test -pl game-service` — **185/185**; весь multi-module — **198/198**.

## Задачи Sprint 5 (ТЗ, раздел 14) — Frontend: игровой цикл

- S5.1 ✅ — API-обёртка на fetch + типы ответов бэкенда (ТЗ 8.5): `frontend/src/api/types.ts`, `frontend/src/api/client.ts` (startGame, getGame, getMoves, makeMove, makeAiMove, resign, offerDraw, restartGame, getLlmModels, likeComment); все DTO типизированы, ошибки — через `ApiError`. (коммит ca15084)
- S5.2 ✅ — компоненты Board/Piece/Highlight + рендер из состояния бэкенда (ТЗ 8.1): `frontend/src/components/game/Board.tsx`, `Cell.tsx` (доска 8×8 CSS Grid, клики, подсветка допустимых ходов, анимация перемещения). (коммит ca15084)
- S5.3 ✅ — панель настроек: ColorSelector, DifficultySelector, ModelSelector, StyleSelector (ТЗ 8.1): `frontend/src/components/game/SettingsPanel.tsx`. (коммит ca15084)
- S5.4 ✅ — компоненты StatusBar, CommentaryBox, GameControls (ТЗ 8.2): `frontend/src/components/game/{StatusBar,CommentaryBox,GameControls}.tsx` (счёт шашек, комментарий ИИ с эмодзи и источником, сдача/ничья/рестарт). (коммит ca15084)
- S5.5 ✅ — App.tsx: состояние партии, ход игрока/ИИ, интеграция всех компонентов, история ходов. (коммит ca15084)
- S5.6 ✅ — UI-тесты: `frontend/src/App.test.tsx` (5 тестов), `frontend/src/components/game/Board.test.tsx` (5 тестов). `npm test` — 10/10, BUILD SUCCESS. (коммит ca15084)

## Задачи Sprint 6 (ТЗ, раздел 14) — Анимации и визуальная подача

- S6.1 ✅ — анимация перемещения и прыжков (ТЗ 8.4): `frontend/src/App.tsx` (вычисление анимации между состояниями доски), `frontend/src/components/game/Cell.tsx` (CSS-анимация movePiece, jumpPiece), `frontend/src/App.css` (ключевые кадры movePiece, jumpPiece). (коммит a32ad0b)
- S6.2 ✅ — корона при превращении + пульсация выбранной шашки (ТЗ 8.4): `frontend/src/App.css` (ключевые кадры crownAppear, pulseCell, cellFlash), `frontend/src/components/game/Cell.tsx` (классы piece--promoting, piece--capture-jump). (коммит a32ad0b)

## Задачи Sprint 7 (ТЗ, раздел 14) — Docker и интеграция

- S7.1 ✅ — docker-compose.yml: db, game-service, llm-service, frontend (порт 80), сеть, env, depends_on с healthcheck. (коммит a32ad0b)
- S7.2 ✅ — Dockerfile для game-service (многоэтапная сборка, eclipse-temurin:21), Dockerfile для llm-service, Dockerfile для frontend (node:20-alpine + nginx). (коммит a32ad0b)
- S7.3 ✅ — nginx конфигурация: статика + прокси `/api` → game-service:8080. (коммит a32ad0b)

## Задачи Sprint 8 (ТЗ, раздел 14) — Стабилизация и приёмка

- S8.1 ✅ — балансировка глубины/бюджета; профилирование (R-7 из B4): `AiPerformanceBenchmarkTest` — EASY 17мс/500мс, MEDIUM 30мс/1500мс, HARD 50мс/3000мс, CUSTOM(8/2000) 265мс/2000мс, CUSTOM(10/3000) 667мс/3000мс — все укладываются в бюджет. (коммит a32ad0b)
- S8.2 ✅ — стресс-тест конкурентности партий: `ConcurrentAiMoveStressTest` — 20 потоков × 5 запросов ai-move: 1 success, 95 AI_BUSY; 10 потоков human-move: 1 success, 9 STALE_STATE/NOT_YOUR_TURN; партия не сломана, moveNumber корректен. (коммит a32ad0b)
- S8.3 ✅ — приёмочный прогон (раздел 13): все критерии приёмки проверены — правила игры, выбор цвета/сложности/модели, ход ИИ в бюджете, комментарий с fallback, коды ошибок, персистентность в tournament, интерфейс с анимациями, Docker.
- S8.4 ✅ — финальная сверка артефактной памяти: все файлы памяти актуальны (STATE.md, FEATURES.md, DECISIONS.md, RUNBOOK.md, BACKLOG.md).

## Задачи Sprint 9 (после S8) — стабилизация Docker-запуска

- S9.1 ✅ — сборка backend-образов: контекст `./backend` (Maven-агрегатор), `maven:3.9-eclipse-temurin-21-alpine`, BuildKit-кэш `~/.m2`, jar из `<module>/target`, `backend/.dockerignore`. Файлы: `docker-compose.yml`, `backend/game-service/Dockerfile`, `backend/llm-service/Dockerfile`, `backend/.dockerignore`.
- S9.2 ✅ — frontend в Docker: `node:22-alpine` + `npm ci` по `package-lock.json`, `node_modules`/`dist` исключены; сервис вынесен в профиль `prod` (обычный `docker compose up -d` поднимает только db + game-service + llm-service). Файлы: `frontend/Dockerfile`, `frontend/.dockerignore`, `docker-compose.yml`.
- S9.3 ✅ — дефекты `llm-service`: `@Autowired` на публичном конструкторе `OllamaClient` (было два конструктора → «No default constructor found»), `messages` вместо `prompt` в `/api/chat` (Ollama отвечала пустым `content`), корректные `Duration`-ключи в `application.yml`, склейка токенов в одно SSE-событие.
- S9.4 ✅ — дефекты `game-service`: SSE-парсер понимает `data:` без пробела (формат Spring WebFlux), пустые события → `null` → fallback; тесты `LlmServiceClientTest` (9), `OllamaClientTest` (6), новый `LlmServiceControllerTest` (3), герметичность `GameApiIntegrationTest` (`llm.enabled=false`). Итог: **208/208**.

## Задачи Sprint 10 (UI/UX) — редизайн интерфейса

- S10.1 ✅ — исправлена сетка доски: `.board` был grid на 8 колонок с тремя детьми (подписи + `.board__grid`), у которого не было `display: grid` — буквы/цифры шли столбиком. Теперь `.board__squares` — настоящая сетка 8×8, координаты вынесены в `.board__ranks`/`.board__files`, добавлены переворот доски под цвет игрока и подсветка последнего хода. Файлы: `frontend/src/components/game/Board.tsx`, `Cell.tsx`, `App.css`.
- S10.2 ✅ — новый макет: sticky-хедер, три колонки (настройки слева, доска в центре, комментарий и история справа), адаптив `<1280px` → 2 колонки, `<960px` → 1 колонка; токены light/dark через `prefers-color-scheme`, базовый сброс Vite-шаблона. Файлы: `frontend/src/index.css`, `frontend/src/App.css`, `frontend/src/App.tsx`.
- S10.3 ✅ — настройки в левом сайдбаре: segmented-выбор цвета и сложности, поля глубины/бюджета для «Свои», стиль и модель LLM, всегда доступны, кнопка «Новая партия» (рестарт убран из блока управления). Файл: `frontend/src/components/game/SettingsPanel.tsx`, `GameControls.tsx`.
- S10.4 ✅ — живой счёт шашек из токенов доски, карточки игроков с индикатором хода, история ходов через `GET /api/game/{id}/moves` в нотации `c3–d4`/`c3×e5`, статус с результатом и причиной окончания. Файлы: `frontend/src/App.tsx`, `frontend/src/components/game/{PlayerCard,MoveHistory,StatusBar}.tsx`.
- S10.5 ✅ — исправлены дефекты: простые шашки (токены `w`/`b`) не выбирались (проверка искала только `W`/`B`), рестарт создавал вторую партию, при ходе ИИ подсвечивались его ходы, счёт был заглушкой «— : —», состояние лайка не синхронизировалось с новым комментарием; добавлена защита от устаревших ответов API. Файлы: `frontend/src/App.tsx`, `frontend/src/components/game/{Board,CommentaryBox}.tsx`, `frontend/src/components/game/tokens.ts`.
- S10.6 ✅ — тесты: `App.test.tsx` (7), `App.game.test.tsx` (2), `Board.test.tsx` (6), `SettingsPanel.test.tsx` (7), `MoveHistory.test.tsx` (4), `tokens.test.ts` (3); удалён неиспользуемый стаб `components/Board.tsx`. Итог: `npm test` — **29/29**, `npm run build` — OK, e2e в браузере (старт за чёрных → ход ИИ → выбор шашки → ход человека → ответ ИИ с комментарием).

## Задачи Sprint 0 (завершён)

- S0.1 ✅ — backend: чистка дублей вне `src`, слияние конфигов, запуск + подключение к `db`. (коммит `2d6ce68`)
- S0.2 ✅ — frontend: убрать демо-`items`, добавить заглушку доски. (коммит `b9c262f`)
- S0.3 ✅ — `docker compose up -d db`, проверка Flyway на `V1__create_items.sql`. (без кодовых изменений — верификация, см. FEATURES.md)
- S0.4 ✅ — актуализировать `docs/memory/{STATE,FEATURES,DECISIONS,RUNBOOK,BACKLOG}.md`, связать с `AGENTS.md`. (коммит `1632b17` + этот docs-коммит)
- S0.5 ✅ — тесты/линтеры (JUnit, Vitest/RTL), Conventional Commits. (коммит `5b935f8`)

## Блокеры

- нет

## Все фичи завершены

| ID | Название | Статус | Коммит |
|----|----------|--------|--------|
| F-001 | Модель игрового ядра | Done | `501e6fa` |
| F-002 | Единый генератор ходов | Done | `d7fe4b3` |
| F-003 | Правило большинства и превращение | Done | `f638813` |
| F-004 | Валидация пути и применение хода | Done | `c0ae6c6` |
| F-005 | Определение конца партии | Done | `7d3f10f` |
| F-006 | Спецправила и закрытие Sprint 1 | Done | `033bdc0` |
| F-007 | Оценка позиции для минимакса | Done | `4d0ae31` |
| F-008 | Минимакс с альфа-бетой | Done | `e36fe33` |
| F-009 | Транспозиционная таблица на Zobrist | Done | `209806b` |
| F-010 | Обрезка ничьих внутри поиска | Done | `d0bec33` |
| F-011 | Эталонные позиции и бенчмарк ИИ | Done | `33b64c1` |
| F-012 | Исправление R-1 | Done | `0cd8ebb` |
| F-013 | Реестр партий | Done | `82a4b90` |
| F-014 | Персистентность результатов | Done | `2e02bf9` |
| F-015 | REST-контракт партии | Done | `0a7f031` |
| F-016 | Микросервис llm-service к Ollama | Done | S4.1 |
| F-017 | Интеграция game-service с llm-service | Done | S4.2 |
| F-018 | Комментарии со стилем и эмоцией | Done | S4.3 |
| F-019 | Сохранение комментариев в llm_comments | Done | S4.4 |
| F-020 | Лайки комментариев LLM | Done | S4.5 |
| F-021 | Фронтенд: игровой цикл | Done | S5.1–S5.6 |
| F-022 | Анимации и визуальная подача | Done | S6.1–S6.2 |
| F-023 | Docker и интеграция | Done | S7.1–S7.3 |
| F-024 | Стабилизация Docker-запуска и дефекты интеграции LLM | Done | S9.1–S9.4 |
| F-025 | UI/UX-редизайн: макет, доска, тема | Done | S10.1–S10.6 |

## Статус проекта: ВСЕ ЗАДАЧИ ЗАВЕРШЕНЫ ✅

- Все спринты (0–10) выполнены
- Все фичи (F-001…F-025) реализованы и протестированы
- Все тесты зелёные: backend **208/208** (game-service 191 + llm-service 17), frontend **29/29**
- Docker: `docker compose up -d` — db + game-service + llm-service (healthy); `docker compose --profile prod up -d` — добавляет frontend на http://localhost/ (после изменения UI образ frontend нужно пересобрать: `docker compose --profile prod build frontend`)
- Критерии приёмки (раздел 13 ТЗ) выполнены
- Бэкенд: 8080, Фронтенд: 5173 (dev) / 80 (Docker), LLM-сервис: 8081
- БД: PostgreSQL 16, порт 5432

## Ссылки (ключевые файлы)

- ТЗ: `game-chashki-TZ.md`
- Правила проекта: `AGENTS.md`
- Бэклог: `docs/memory/BACKLOG.md`
- Журнал фич: `docs/memory/FEATURES.md`
- Решения: `docs/memory/DECISIONS.md`
- Запуск: `docs/memory/RUNBOOK.md`
- Ревью Sprint 0–2: `docs/memory/REVIEW-sprint-0-2.md`