# Решения (ADR)

Каждое значимое техническое решение фиксируется здесь: контекст, решение, альтернативы, следствия.

## ADR-0001. Живое состояние партии — в памяти

- **Дата:** 2026-10-17
- **Контекст:** MVP для одного пользователя, один экземпляр бэкенда.
- **Решение:** живое состояние партий в `ConcurrentHashMap` + TTL-вытеснение; в БД живое состояние не хранится.
- **Альтернативы:** хранение живых партий в PostgreSQL (отклонено — избыточно для активных партий).
- **Следствия:** незавершённые партии теряются при рестарте; горизонтальное масштабирование не поддерживается. Результаты завершённых партий персистентны — см. ADR-0004.

## ADR-0002. Базовый путь API — `/api`

- **Дата:** 2026-10-17
- **Контекст:** согласование с `AGENTS.md`.
- **Решение:** эндпоинты партий под `/api/game/...`, конфигурация — `/api/config/...`.
- **Следствия:** nginx/дев-прокси настроены на префикс `/api`.

## ADR-0003. LLM — отдельный `llm-service` поверх Ollama, модель задаётся параметром

- **Дата:** 2026-10-17
- **Контекст:** аппаратный профиль — CPU-only (Ryzen 7 8845HS, iGPU Radeon 780M, 32 ГБ ОЗУ).
- **Решение:** микросервис-прокси к Ollama; модель через `LLM_MODEL`, по умолчанию `qwen2.5:7b-instruct`; выбор модели в UI; таймаут + обязательный fallback.
- **Альтернативы:** вызов Ollama напрямую из Java (отклонено — хуже изоляция провайдера).
- **Следствия:** обязателен детерминированный fallback-комментарий; модели ≥14B не используются.

## ADR-0004. PostgreSQL для результатов партий и избранных комментариев

- **Дата:** 2026-10-18
- **Контекст:** требование сохранять результат завершённой партии и (в перспективе) избранные комментарии LLM.
- **Решение:** PostgreSQL 16 + Flyway. Таблица `tournament` (результат завершённой партии) реализуется в основном объёме; таблица `llm_comments` проектируется сейчас, запись — в Бэклоге (B2). Запись результата — best-effort, не ломает партию при сбое БД.
- **Альтернативы:** без персистентности; хранение в памяти (отклонено — результат должен переживать перезапуск).
- **Следствия:** схема — только через новые Flyway-миграции; `ddl-auto: validate`; доступ к данным через JPA-репозитории.

## ADR-0005. Стек выровнен на существующий каркас — Spring Boot 4.1.1

- **Дата:** 2026-10-18
- **Контекст:** рабочий каркас использует Spring Boot 4.1.1; правило в `AGENTS.md` упоминало 3.x.
- **Решение:** переиспользуем существующий каркас; правило `AGENTS.md` обновлено на «актуальный Spring Boot 4.x (4.1.1)».
- **Следствия:** Java 21, `jakarta.*`, Spring Data JPA, Flyway, драйвер PostgreSQL — из каркаса.

## ADR-0006. Backend: `src` — единственный source root, `application.yml` — единственный конфиг

- **Дата:** 2026-10-18 (Sprint 0, S0.1)
- **Контекст:** в каркасе лежали дубли Java-файлов вне `src` (`config/CordConfig.java`, `controller/`, `dto/`, `repository/`) и два конфига (`application.properties` + `application.yml`).
- **Решение:** удалены все дубли вне `src` (содержимое совпадало с `src`); `application.properties` удалён, его единственное содержимое (`spring.application.name`) уже было в `application.yml`.
- **Альтернативы:** оставить оба конфига (отклонено — неявный приоритет и риск рассинхрона).
- **Следствия:** вся конфигурация backend — в `backend/src/main/resources/application.yml`; секреты и локальные переопределения — только через переменные окружения (см. RUNBOOK).

## ADR-0007. Frontend: нативный `fetch` вместо axios

- **Дата:** 2026-10-18 (Sprint 0, S0.2)
- **Контекст:** каркас использовал `axios` только для демо-`items`; ТЗ (раздел 8.5) требует нативный `fetch` с типизированной обёрткой «без лишних зависимостей».
- **Решение:** `axios` удалён из зависимостей вместе с демо-интерфейсом; API-вызовы будут идти через типизированную обёртку над `fetch`.
- **Следствия:** обёртка над `fetch` создаётся при подключении API-интеграции (Sprint 3+); у frontend одна рантайм-зависимость — React.

## ADR-0008. Тестовая инфраструктура: JUnit 5 против реального `db`, Vitest + RTL + jsdom

- **Дата:** 2026-10-18 (Sprint 0, S0.5)
- **Контекст:** ТЗ (раздел 14, Sprint 0 п.5) требует настроить тесты (JUnit, Vitest/RTL) и линтеры.
- **Решение:** backend — JUnit 5: `@SpringBootTest` (контекст + реальный `db` из docker) и чистые unit-тесты; frontend — Vitest 5 + React Testing Library + jsdom, отдельный `vitest.config.ts`, setup-файл `src/test/setup.ts`; линтер — oxlint (уже был).
- **Альтернативы:** Testcontainers (отклонено — избыточно для MVP, `docker compose up -d db` уже поднимает БД); H2 вместо PostgreSQL (отклонено — тесты должны видеть реальную схему Flyway).
- **Следствия:** тесты backend требуют запущенного `db` (см. RUNBOOK); `npm run build` типизирует и тесты (`tsc -b` включает `src`).

## ADR-0009. Игровое ядро — пакет `com.example.backend.game`

- **Дата:** 2026-10-18 (Sprint 1, S1.1)
- **Контекст:** в каркасе уже есть демо-код `items` (controller/dto/repository); движок правил должен жить отдельно и не зависеть от него.
- **Решение:** весь rules engine — в пакете `com.example.backend.game` (`Board`, `Piece`, `GameState`, ...); демо-`items` остаются до внедрения игрового API (Sprint 3) и будут удалены при рефакторинге `ItemController`.
- **Следствия:** ядро — чистая Java без Spring-зависимостей (тестируется как unit-тесты); API/персистентность подключаются отдельными слоями поверх.

## ADR-0010. Push на GitHub — PAT из `.secrets` через локальный credential helper

- **Дата:** 2026-10-18 (Sprint 1)
- **Контекст:** нужно, чтобы аутентификация `git push` шла именно по PAT из `.secrets`, а не через сохранённые ранее креды Git Credential Manager (системный helper `manager`).
- **Решение:** в `.git/config` репозитория записано: `credential.helper = ""` (сброс системного `manager` при чтении) и `credential.helper = !f() {...}` — скрипт выдаёт `username=AlexOstrov` и `password` = значение `token:` из `D:/projects/fullstack-demo/.secrets`.
- **Следствия:** helper действует только для этого репозитория; `.secrets*` не попадает в git (`.gitignore`); значение helper записывалось через `git config --local --add` — прямая raw-запись в `.git/config` git-парсер обрезает на первой `;` (нужно экранирование кавычками, которое git делает сам при `--add`).

## ADR-0011. Расширенные многострочные commit-сообщения

- **Дата:** 2026-10-18 (Sprint 1, S1.6)
- **Контекст:** пожелание — commit-сообщения должны раскрывать содержание задачи, как подведение итогов в отчёте, а не быть одной строкой.
- **Решение:** сохранять формат Conventional Commits на русском (`feat(game): ...`), но в тело сообщения добавлять структурированное содержание: что сделано (функциональные пункты), где (файлы/модули), чем проверено (тесты/DoD). Заголовок — сжатый итог, тело — детали.
- **Следствия:** сообщения длиннее (5–10 строк); `git log --oneline` даёт заголовок, полное содержание — в `git log`. Первые коммиты спринтов (до S1.6) остались однострочными — не переписываем историю.

## ADR-0012. Сдача и ничья по согласию — методы движка, учёт предложений — API-слой

- **Дата:** 2026-10-18 (Sprint 1, S1.6)
- **Контекст:** ТЗ 3.6 относит сдачу и ничью по согласию к окончанию партии, а ТЗ 12 — к юнит-тестам правил; при этом учт«предложение ничьей → принятие/отказ» — это межигровое взаимодействие (Sprint 3 API).
- **Решение:** в `GameState` добавлены `resign(Color)` (поражение сдавшегося) и `drawByAgreement()` (фиксация `DRAW`) с общей проверкой `checkInProgress`; оба инкрементируют `version`. Учёт предложений/ответов на ничью остаётся в сервисном слое API (Sprint 3), он вызовет эти методы.
- **Следствия:** движок полон по разделу 3 ТЗ; API-слой не дублирует логику статусов, а управляет только состоянием «предложена ничья».

## ADR-0013. Эвристика оценки позиции и пакет `game.ai`

- **Дата:** 2026-10-18 (Sprint 2, S2.1)
- **Контекст:** ТЗ 6 задаёт компоненты оценки (материал, продвижение, безопасность, мобильность, центр, темп) и фиксированный материал (шашка 100, дамка 300), но не задаёт веса прочих компонентов; ИИ-слой должен остаться чистой Java (ADR-0009).
- **Решение:** класс `PositionEvaluator` в новом пакете `com.example.backend.game.ai` (чистая Java без Spring). Веса: продвижение +4/клетку к превращению; борт (col 0/7) +8; тыл (задние ряды) +5; «под боем» −20 (штраф меньше цены шашки: при правиле большинства взятие может быть вынужденно отложено); мобильность — разница числа ходов ×2; центр дамки 2×(8 − Чебышёв_к_центру) = 2..14; темп +5. Оценка — с позиции стороны, чей ход (негамма-готово для минимакса). Детектор «под боем»: первый непустой по диагонали — соперник; шашка-нападающий только при дистанции 1 до жертвы (прыжок через соседнюю), дамка — с любой; за жертвой есть свободное приземление.
- **Следствия:** значения зафиксированы тестами точными дельтами (`PositionEvaluatorTest`) — изменение весов потребует обновления тестов; мобильность считает полную генерацию ходов обеих сторон на каждом листе — узкое место для бюджета времени, при необходимости оптимизируется в S2.5 (бенчмарк). Решение принято по spirit DoD Sprint 2: «DECISIONS.md при выборе значений эвристики».

## ADR-0014. Ревью Sprint 0–2 и контракт учёта повторений в поиске

- **Дата:** 2026-10-18 (ревью завершённых спринтов 0–2)
- **Контекст:** по запросу владельца проекта проведено ревью кода и соответствия ТЗ для завершённых спринтов; отчёт — `docs/memory/REVIEW-sprint-0-2.md`.
- **Решение:** замечание R-1 (двойной учёт корневой позиции при обрезке ничьей в `AlphaBetaSearch`) исправлено: корень больше не добавляется в `pathPositions`, его кратность учитывает `gameRepetitions`. Контракт: `positionRepetitions` (`GameState.positionRepetitions()`) включает текущую позицию; `pathPositions` — только узлы ниже корня. Прочие замечания R-2…R-7 вынесены в бэклог B4.
- **Альтернативы:** оставить как есть (отклонено — ложная ничья на повтор раньше реального в эндшпилях при подключении API в S3).
- **Следствия:** добавлен регрессионный тест `AlphaBetaSearchTest#rootPositionIsNotCountedTwiceForRepetitionPruning` (до фикса падает); при подключении истории в S3 передавать карту, включающую текущую позицию.

## ADR-0015. API-слой поверх игрового ядра и единый формат ошибок

- **Дата:** 2026-10-18 (Sprint 3, S3.2–S3.3)
- **Контекст:** ТЗ 5 задаёт REST-контракт и единый формат ошибки с кодами; ядро (ADR-0009) — чистая Java без Spring, поэтому транспорт/валидация/ошибки не должны проникать в `game`.
- **Решение:** API-слой — отдельные пакеты: `controller` (GameController, ConfigController), `dto.game` (records + Jakarta Validation, энумы PlayerColor/Difficulty/CommentaryStyle/CommentSource), `error` (ApiErrorCode, ApiException, GlobalExceptionHandler, ApiErrorResponse), `service` (GameService, мапперы, каталог моделей); `session` (GameSession/GameRegistry). Коды ТЗ 5.3: `GAME_NOT_FOUND` → 404, `AI_BUSY`/`STALE_STATE` → 409 (конфликт с текущим состоянием, запрос повторяем), `NOT_YOUR_TURN`/`INVALID_MOVE`/`GAME_FINISHED`/`VALIDATION_ERROR` → 400, плюс `INTERNAL_ERROR` → 500 для непредвиденных сбоев. Ответы — records; null-поля не сериализуются (`spring.jackson.default-property-inclusion: non_null`), поэтому `ai` присутствует только в ответе `ai-move` (ТЗ 5.2). Ход игрока: `clientMoveNumber` сверяется с `moveNumber` (ТЗ 5.4); ход ИИ вызывается только при очереди ИИ.
- **Альтернативы:** валидация в контроллерах и обработка ошибок в каждом методе (отклонено — дублирование); ошибки через `ResponseStatusException` без кодов (отклонено — ТЗ требует `code`).
- **Следствия:** добавление эндпоинтов/кодов — только через `ApiErrorCode` и `GlobalExceptionHandler`; ядро от API не зависит. Формат значения enum в JSON — имя константы (`MEDIUM`, `FRIENDLY_MASTER`); строчный токен стиля (`friendly_master`) для контракта LLM даёт `CommentaryStyle.token()` (Sprint 4).

## ADR-0016. Решение ИИ по ничьей и семантика `restart`

- **Дата:** 2026-10-18 (Sprint 3, S3.2)
- **Контекст:** ТЗ 5 объявляет `POST /draw` как «предложить/принять ничью», но не задаёт политику ИИ и протокол (кто и когда принимает). `tournament.game_id` — UNIQUE, поэтому «заново» нельзя переиспользовать старый id при повторной записи результата.
- **Решение:** `draw` — одномоментное решение: игрок предлагает, сервер сразу оценивает позицию статической эвристикой с точки зрения ИИ и принимает, если оценка не лучше `DRAW_ACCEPT_THRESHOLD = 50` (половина простой шашки); при согласии движок фиксирует `DRAW`/`DRAW_AGREEMENT` и результат сохраняется. `restart` создаёт **новую партию** (новый `gameId`); тело опционально — без него берутся прежние настройки (`GamePreferences`). Старая партия остаётся в реестре до TTL.
- **Альтернативы:** двухфазный протокол «предложено/принято» с хранением флага в сессии (отклонено — избыточно для MVP, ТЗ не требует); переиспользование id при рестарте (отклонено — нарушает UNIQUE и «одна строка = одна партия»).
- **Следствия:** политика ничьей детерминирована и покрыта тестами (принятие на равной позиции, отказ при перевесе ИИ); UI должен трактовать ответ `{accepted, game}`; смена семантики рестарта потребует правок документации API.

## ADR-0017. Env-конфигурация пресетов ИИ, TTL и удаление демо-`items`

- **Дата:** 2026-10-18 (Sprint 3, S3.1/S3.4, замечания R-3/R-4/R-6)
- **Контекст:** ревью S0–S2 (R-3) отметило, что пресеты ИИ захардкожены и ТЗ 6.1/§9 требует `AI_PRESET_*` и `GAME_TTL_MINUTES`; R-4 — нужен явный маппинг `Difficulty.CUSTOM → AiSettings`; ADR-0009/R-6 — демо-`items` удаляются в Sprint 3.
- **Решение:** `@ConfigurationProperties` (`AiProperties`, `GameProperties`, `LlmProperties`). Значения по умолчанию в `application.yml`, переопределение — env через relaxed binding: `ai.preset.<easy|medium|hard>.depth` ← `AI_PRESET_EASY_DEPTH` и т. п., `AI_TT_CAPACITY`, `GAME_TTL_MINUTES`, `GAME_CLEANUP_INTERVAL_MS`, `LLM_*`, `OLLAMA_BASE_URL`. Маппинг сложности — `AiSettingsFactory`: пресеты из конфигурации, `CUSTOM` — из полей запроса (`@Min/@Max` в DTO + проверка в фабрике). Демо-`items` (controller/dto/model/repository + тест) удалены, таблица — миграцией `V2__drop_items.sql` (применённые миграции не редактируются).
- **Альтернативы:** оставить пресеты в `AiPreset` (отклонено — ТЗ требует конфигурируемость); править `V1__create_items.sql` (отклонено — AGENTS запрещает).
- **Следствия:** изменение пресетов/TTL — без правок кода; `AiPreset` остаётся дефолтами для тестов/движка, но источник значений для партии — свойства.

## ADR-0018. Микросервис-прокси `llm-service` поверх Ollama (Sprint 4, S4.1)

- **Дата:** 2026-10-18 (Sprint 4, S4.1)
- **Контекст:** ТЗ 7 требует микросервис-прокси к Ollama: модель по умолчанию `qwen2.5:7b-instruct`, список моделей = пересечение Ollama `/api/tags` и `LLM_ALLOWED_MODELS`. Backend разбит на multi-module (`game-service` + `llm-service`), `llm-service` на порту 8081.
- **Решение:** `llm-service` — Spring Boot WebFlux приложение. `OllamaClient` (WebClient к `Ollama /api/tags` и `/api/chat` NDJSON) с таймаутами через `OllamaProperties` (env `OLLAMA_BASE_URL`, connect/chat/tags таймауты). `ModelCatalog` — кэш моделей = Ollama ∩ `LLM_ALLOWED_MODELS` (env `LLM_ALLOWED_MODELS`), обновляется через `@PostConstruct` + `@Scheduled`. `LlmChatClient` — проверка allowlist моделей. `LlmServiceController` — `GET /api/models` и `POST /api/chat` (TEXT_EVENT_STREAM). DTO `ChatRequest`/`ModelsResponse` (Jakarta Validation). Конфиг: `llm.ollama.*`, `app.llm.*` (env `LLM_MODEL`, `LLM_ALLOWED_MODELS`).
- **Альтернативы:** вызовы Ollama напрямую из `game-service` (отклонено — хуже изоляция провайдера, отдельный порт не нужен, но ADR-0003 зафиксировал отдельный сервис); JSON-ответ вместо streaming (отклонено — TZ требует streaming для комментариев).
- **Следствия:** `backend/pom.xml` — aggregator (packaging pom, модули `game-service` и `llm-service`); код игры перенесён в `backend/game-service/`; `mvnw`/`mvnw.cmd`/`.mvn` скопированы в корень `backend/`; тесты `llm-service` — 13/13, весь multi-module — 174/174; `GET /api/config/llm-models` в `game-service` пока возвращает статичный allowlist (S4.2 подключит динамику через `llm-service`).

## ADR-0019. Интеграция `game-service` с `llm-service` (Sprint 4, S4.2)

- **Дата:** 2026-10-19 (Sprint 4, S4.2)
- **Контекст:** после создания `llm-service` (S4.1) нужно подключить живой LLM к комментариям хода ИИ в `game-service`. ТЗ 7 требует комментарии к ходу ИИ с возможностью fallback при недоступности LLM.
- **Решение:** `LlmServiceClient` (RestClient к `llm-service:8081/api/chat`, SSE-парсинг `data: ...`, таймауты из `LlmProperties`); `LlmCommentProvider` реализует `CommentProvider` — запрашивает комментарий у LLM, при `LLM_ENABLED=false`, таймауте или сбое переходит на `FallbackCommentProvider`; `LlmComment` помечается `fallback=false` + модель при успехе, `fallback=true` без модели при фолбэке. Бины настраиваются в `GameConfiguration` (`llmServiceClient`, `fallbackCommentProvider`, `commentProvider`). Промпт содержит доску, цвет ИИ, номер хода, тип хода, стиль комментария.
- **Альтернативы:** оставить только fallback (отклонено — ТЗ требует живого LLM); синхронный HTTP без SSE (отклонено — TZ требует streaming для `/api/chat`).
- **Следствия:** `FallbackCommentProvider` убран из `@Service` (создаётся как `@Bean` в `GameConfiguration`); `CommentProvider` — основной бин через `GameConfiguration`; тесты `LlmServiceClientTest` (6) + `LlmCommentProviderTest` (5); `game-service` — 172/172, весь multi-module — 185/185.

## ADR-0020. Стиль и эмоция комментария LLM (Sprint 4, S4.3, R-5 из B4)

- **Дата:** 2026-10-19 (Sprint 4, S4.3)
- **Контекст:** ревью Sprint 0–2 (R-5) требует расширение `LlmComment` полями `style`/`emotion`. Стиль уже существовал в `CommentaryStyle` (enum в `dto.game`), но не передавался в `LlmComment`. Нужна эмоциональная окраска комментария для UI (иконка, оформление).
- **Решение:** создан enum `Emotion` (`NEUTRAL`, `EXCITED`, `CONFIDENT`, `CAUTIOUS`) с токенами в `dto.game`; `LlmComment` расширен полями `style` (`CommentaryStyle`) и `emotion` (`Emotion`); `LlmCommentProvider` и `FallbackCommentProvider` передают `style`/`emotion` из `session.preferences().commentaryStyle()`; маппинг стиля на эмоцию: FRIENDLY_MASTER→NEUTRAL, COMPETITIVE→CONFIDENT, NEUTRAL→NEUTRAL. Интерфейс `CommentProvider` не меняется (сигнатура `comment(GameSession, Move)` оставлена без изменений — style/emotion определяются внутри провайдеров из сессии).
- **Альтернативы:** добавить `style`/`emotion` как параметры в интерфейс `CommentProvider` (отклонено — нарушает контракт, все вызывающие должны бы меняться); хранить эмоцию отдельно в БД (отклонено — часть комментария, должна быть в `LlmComment`).
- **Следствия:** `LlmComment` record расширен (обратная несовместимость для сериализации JSON — новые поля добавляются в конец, null-поля не сериализуются); тесты `LlmCommentProviderTest` расширены до 6 (вкл. `emotionOfMapsStyleToEmotion`); `game-service` — 173/173, весь multi-module — 186/186.

## ADR-0021. Сохранение комментариев LLM в `llm_comments` (Sprint 4, S4.4, B2)

- **Дата:** 2026-10-20 (Sprint 4, S4.4)
- **Контекст:** бэклог B2 требует сохранения комментариев LLM в таблицу `llm_comments` (создана миграцией V4). Таблица изначально не имеет колонки `game_id` — нужна привязка комментария к партии для будущих запросов (логи B2/B3).
- **Решение:** `LlmCommentEntity` (JPA сущность таблицы `llm_comments` с полями comment, style, emotion, llm_model, move_number, created_at, game_id); `LlmCommentRepository` (JpaRepository + `findByGameId`/`deleteByGameId`); `LlmCommentRecorder` (best-effort сервис, как `TournamentRecorder` — сбой БД логируется, партию не ломает). Интеграция в `GameService.aiMove()` — `llmCommentRecorder.record(session, comment)` после генерации комментария. Миграция `V5__add_game_id_to_llm_comments.sql` добавляет колонку `game_id` и индекс (нельзя редактировать применённую V4 по AGENTS.md).
- **Альтернативы:** сохранить комментарий в `tournament` (отклонено — разная сущность и семантика); сохранять только избранные комментарии (отклонено — B2 требует сохранение всех, лайки — отдельная история B2 UI-часть).
- **Следствия:** `GameService` конструктор расширен `LlmCommentRecorder`; `GameServiceTest` обновлён (мок `LlmCommentRepository`, проверка `save`); `LlmCommentRecorderTest` (4 теста); `game-service` — 177/177, весь multi-module — **190/190**.

## ADR-0022. Лайки комментариев LLM (Sprint 4, S4.5, B2)

- **Дата:** 2026-10-20 (Sprint 4, S4.5)
- **Контекст:** бэклог B2 требует лайки комментариев LLM: пиктограммы «палец вверх/вниз» рядом с комментарием; «вверх» → комментарий сохраняется/остаётся в `llm_comments`, «вниз» → комментарий удаляется из `llm_comments` (не сохраняется).
- **Решение:** `LikedRequest` DTO с `@NotNull liked` (Jakarta Validation); `LlmCommentService.toggleLike(gameId, commentId, liked)` — liked=true вызывает `repository.save`, liked=false вызывает `repository.delete`; `LlmCommentController` — `POST /api/game/{id}/comments/{commentId}/like` → 204 No Content / 404 Not Found; `LlmCommentEntity.liked` + миграция `V6__add_liked_to_llm_comments.sql` (колонка `liked` boolean + индекс); `LlmComment.liked`, `AiTurnDto.liked`, `CommentDto.liked` в ответах API. Все автосохранённые комментарии получают `liked=true` по умолчанию.
- **Альтернативы:** отдельный enum для лайка (отклонено — boolean достаточно для да/нет); хранить лайки в отдельной таблице `llm_comment_likes` (отклонено — проще использовать флаг на самой записи комментария).
- **Следствия:** `GameService.aiMove()` уже сохраняет комментарий через `LlmCommentRecorder` (S4.4); лайк просто обновляет существующую запись; `LlmCommentServiceTest` (4 теста), `LlmCommentControllerTest` (3 теста); `game-service` — 185/185, весь multi-module — **198/198**.

## ADR-0023. Docker и инфраструктура (Sprint 7, S7.1–S7.3)

- **Дата:** 2026-10-20 (Sprint 7)
- **Контекст:** ТЗ разделы 9 требует Docker для воспроизводимого запуска: `docker compose up -d` должен поднимать все сервисы (db, backend, frontend, llm-service).
- **Решение:** multi-stage Dockerfile для каждого сервиса (game-service, llm-service, frontend с nginx); `docker-compose.yml` с сетью, env-переменными, depends_on с healthcheck; `LLM_ENABLED=false` по умолчанию в Docker (Ollama запускается на хосте, не в контейнере, поэтому `llm-service` обращается к `host.docker.internal:11434`).
- **Альтернативы:** использовать официальный образ ollama в compose (отклонено — требует GPU/CPU нагрузки и сложнее настройка; simpler оставить Ollama на хосте); использовать docker-compose для только БД (отклонено — ТЗ требует полного стека).
- **Следствия:** `docker-compose.yml` обновлён (добавлены game-service, llm-service, frontend); `backend/game-service/Dockerfile`, `backend/llm-service/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf` созданы; `OLLAMA_BASE_URL` в `llm-service` по умолчанию указывает на `host.docker.internal:11434` (Ollama на хосте).



## ADR-0024. Docker-сборка backend и место frontend в окружении (Sprint 9)

- **Дата:** 2026-10-21 (Sprint 9, стабилизация Docker-запуска)
- **Контекст:** `docker compose up -d --build` не работал: (1) `backend/pom.xml` — Maven-агрегатор, а контекст сборки указывал на папку модуля, где нет родительского POM и обёртки Maven; (2) jar искался в `/app/target`, тогда как при `-pl <module>` артефакт лежит в `<module>/target`; (3) `frontend` копировал в Linux-образ Windows-каталог `node_modules` (`tsc`/`node.exe`), а `package-lock.json` исключался `.dockerignore`; (4) `llm-service` не стартовал (два конструктора у `OllamaClient`, незабинденные `Duration`-параметры); (5) `/api/chat` в Ollama вызывался с полем `prompt` вместо `messages` — Ollama отвечает `200` с пустым `content`, поэтому комментарии молча уходили в fallback; (6) SSE-парсер `game-service` не понимал `data:` без пробела (формат Spring WebFlux) и принимал пустые события за текст.
- **Решение:**
  - контекст сборки обоих backend-сервисов — `./backend`, Dockerfile — `game-service/Dockerfile` и `llm-service/Dockerfile`; в образ копируются POM обоих модулей и `src` только целевого; сборка `mvn -pl <module> -am package -DskipTests`;
  - базовый образ сборки — `maven:3.9-eclipse-temurin-21-alpine` вместо `temurin` + Maven wrapper (убирает скачивание дистрибутива Maven при каждой сборке и SSL-флейки);
  - `RUN --mount=type=cache,target=/root/.m2` — кэш зависимостей между пересборками; `backend/.dockerignore` исключает `**/target`;
  - `frontend` вынесен в профиль `prod`: `docker compose up -d` поднимает db + game-service + llm-service, а production-сборка UI запускается `docker compose --profile prod up -d` (в разработке frontend идёт на хосте через `npm run dev`, порт 5173 — он меняется чаще всего);
  - `frontend/Dockerfile` — `node:22-alpine` + `npm ci` по `package-lock.json`; `package-lock.json` разрешён в `.dockerignore`, `node_modules`/`dist` — исключены.
- **Альтернативы:** один POM на каждый модуль без агрегатора (отклонено — дублирование зависимостей и версий); собирать оба модуля в один образ и выбирать jar через entrypoint (отклонено — нарушает разделение сервисов); ставить Maven wrapper в контейнер и мириться со скачиванием (отклонено — медленно и нестабильно); оставить frontend в обычном `up` (отклонено по требованию сопровождения: UI меняется чаще, пересборка образа на каждый чих избыточна).
- **Следствия:** `docker compose build` — оба образа собираются, `docker compose up -d` — healthy; e2e `ai-move` отдаёт `commentSource=LLM` с корректным текстом, при остановленном `llm-service` — `commentSource=FALLBACK`; `LLM_TIMEOUT_MS=20000` в compose (7B-модель успевает ответить); тесты — 208/208 (game-service 191 + llm-service 17).