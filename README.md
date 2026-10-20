# 🎯 Русские шашки — полноценный игровой проект с LLM-комментатором

[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=java&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.1.1](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=white)](https://reactjs.org/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![Ollama](https://img.shields.io/badge/Ollama-Ready-000000?logo=ollama&logoColor=white)](https://ollama.ai/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 📖 О проекте

**«Русские шашки»** — полноценное полностековое веб-приложение для игры в русские шашки (8×8, обязательное взятие, правило большинства, дамки) с живым комментарием от ИИ-модели через Ollama.

Проект реализован на современном стеке: **Java 21 + Spring Boot 4.1.1** на бэкенде, **React 19 + TypeScript + Vite** на фронтенде, **PostgreSQL 16** для хранения результатов. Всё разворачивается через **Docker Compose** одной командой.

### Основные возможности

| Возможность | Описание |
|-------------|----------|
| 🎮 **Полноценная игра** | Русские шашки по официальным правилам: обязательное взятие, правило большинства, серийные взятия, превращение в дамку (включая превращение в бою) |
| 🤖 **ИИ-оппонент** | Минимакс с альфа-бета отсечением, итеративным углублением, транспозиционной таблицей (Zobrist) и сортировкой ходов. Три уровня сложности: Easy / Medium / Hard + CUSTOM |
| 💬 **Живые комментарии LLM** | ИИ комментирует свои ходы через Ollama (модель `qwen2.5:7b-instruct` по умолчанию). Три стиля комментария: дружелюбный, соревновательный, нейтральный. Работает с fallback при недоступности LLM |
| 👍 **Лайки комментариев** | Пользователь может поставить/снять лайк комментарию ИИ; понравившиеся комментарии сохраняются в БД |
| 💾 **Персистентность** | Результаты завершённых партий сохраняются в PostgreSQL (таблица `tournament`); комментарии LLM хранятся в таблице `llm_comments` |
| 🎨 **Современный UI** | Доска 8×8 на CSS Grid, анимации ходов и взятий, появление короны, пульсация выбранной шашки, адаптивный дизайн |
| 🐳 **Docker-развёртывание** | Все сервисы (БД, бэкенд, LLM-прокси, фронтенд) поднимаются одной командой `docker compose up -d` |

---

## 🏗️ Архитектура

```
┌─────────────────┐     HTTP/REST      ┌──────────────────┐
│   Frontend      │ ◄─────────────────►│  game-service    │
│  (React/Vite)   │   port 80 / 5173   │   (Spring Boot   │
│                 │                    │    4.1.1, 8080)  │
└─────────────────┘                    └──────────────────┘
                                      │
                                      │ HTTP/REST
                                      ▼
                                ┌──────────────────┐
                                │  llm-service     │
                                │  (Spring Boot,   │
                                │   port 8081)     │
                                └────────┬─────────┘
                                         │ HTTP
                                         ▼
                                    ┌─────────┐
                                    │ Ollama  │
                                    │ (LLM)   │
                                    └─────────┘

┌─────────────────┐
│  PostgreSQL 16  │
│   (port 5432)   │
└─────────────────┘
```

### Модули бэкенда (multi-module Maven)

| Модуль | Назначение | Порт |
|--------|------------|------|
| `game-service` | Игровой ядро, REST API, ИИ-оппонент, персистентность | 8080 |
| `llm-service` | Прокси к Ollama, кэш моделей, комментарии LLM | 8081 |

---

## 🚀 Быстрый старт

### Вариант 1: Docker (рекомендуется)

```bash
# 1. Клонировать репозиторий
git clone https://github.com/AlexOstrov/fullstack-demo.git
cd fullstack-demo

# 2. Запустить все сервисы (без Ollama — модель подключается с хоста)
docker compose up -d

# 3. Проверить доступность
#   Frontend:  http://localhost:80
#   Backend:   http://localhost:8080/api/config/llm-models
#   LLM-serv.: http://localhost:8081/actuator/health

# 4. Остановить
docker compose down
```

> **Важно:** Модель Ollama запускается отдельно на хост-машине (`ollama serve`), так как требует доступа к CPU/iGPU. Убедитесь, что Ollama запущен и модель загружена:
> ```bash
> ollama serve
> ollama pull qwen2.5:7b-instruct
> ```

### Вариант 2: Ручной запуск (разработка)

**Требуется:** PostgreSQL 16, Java 21, Node.js 20+, Ollama

```bash
# 1. База данных
docker compose up -d db

# 2. Backend (из корня репозитория)
cd backend
./mvnw -pl game-service spring-boot:run \
  "-Dspring-boot.run.jvmArguments=-Xmx1536m -XX:MaxMetaspaceSize=512m"

# 3. LLM-сервис (в другом терминале)
cd backend
./mvnw -pl llm-service spring-boot:run \
  "-Dspring-boot.run.jvmArguments=-Xmx1024m -XX:MaxMetaspaceSize=256m"

# 4. Frontend
cd frontend
npm run dev   # http://localhost:5173
```

### Переменные окружения

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/app` | URL базы данных |
| `SPRING_DATASOURCE_USERNAME` | `app` | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | `app` | Пароль БД |
| `GAME_TTL_MINUTES` | `60` | Время жизни неактивной партии |
| `LLM_ENABLED` | `true` | Включить живой LLM |
| `LLM_BASE_URL` | `http://localhost:8081` | Адрес llm-service |
| `LLM_MODEL` | `qwen2.5:7b-instruct` | Модель Ollama |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Адрес Ollama |

---

## 📋 API документация

Базовый путь: **`/api`**

### Игровые эндпоинты

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/api/game/start` | Создать партию (`playerColor`, `difficulty`, `commentaryStyle`, `llmModel`) |
| `GET` | `/api/game/{id}` | Текущее состояние партии |
| `GET` | `/api/game/{id}/moves` | История ходов и комментариев |
| `POST` | `/api/game/{id}/move` | Ход игрока (`path`, `clientMoveNumber`) |
| `POST` | `/api/game/{id}/ai-move` | Ход ИИ + комментарий |
| `POST` | `/api/game/{id}/resign` | Сдаться |
| `POST` | `/api/game/{id}/draw` | Предложить ничью |
| `POST` | `/api/game/{id}/restart` | Новая партия |

### Конфигурация

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/config/llm-models` | Список доступных моделей LLM |

### LLM-комментарии

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/api/game/{id}/comments/{commentId}/like` | Лайк / снять лайк комментария |

### Коды ошибок

| Код | HTTP | Описание |
|-----|------|----------|
| `GAME_NOT_FOUND` | 404 | Партия не найдена |
| `AI_BUSY` | 409 | ИИ уже считает ход |
| `STALE_STATE` | 409 | Устаревшее состояние (`clientMoveNumber`) |
| `NOT_YOUR_TURN` | 400 | Не ваша очередь |
| `INVALID_MOVE` | 400 | Недопустимый ход |
| `GAME_FINISHED` | 400 | Партия уже завершена |
| `VALIDATION_ERROR` | 400 | Ошибка валидации |
| `INTERNAL_ERROR` | 500 | Внутренняя ошибка |

---

## 📊 Cкриншоты и демонстрация

*(Добавьте скриншоты интерфейса игры здесь)*

---

## 🛠️ Технологии

### Backend
- **Java 21**, Spring Boot 4.1.1
- Spring Data JPA, Spring Data Flyway
- Spring Web MVC, Jakarta Validation
- PostgreSQL драйвер
- Минимакс с альфа-бета, Zobrist-хеширование

### Frontend
- React 19, TypeScript, Vite
- CSS Grid, CSS-анимации
- Нативный `fetch` (без axios)

### Инфраструктура
- PostgreSQL 16 (Docker)
- Ollama (LLM, на хосте)
- Docker Compose, nginx (прокси фронтенда)

---

## 🧪 Тестирование

```bash
# Backend (требует запущенной БД)
cd backend
./mvnw test

# Frontend
cd frontend
npm test          # однократно
npm run test:watch # наблюдение

# Линт и сборка frontend
cd frontend
npm run lint      # oxlint
npm run build     # tsc -b && vite build
```

**Результаты последнего прогона:**
- Backend: **201/201 тестов** (game-service 188 + llm-service 13)
- Frontend: **10/10 тестов**
- Сборка: **BUILD SUCCESS**

---

## 📁 Структура репозитория

```
fullstack-demo/
├── backend/                      # Многослойный бэкенд (Maven multi-module)
│   ├── game-service/             # Игровой ядро + REST API (порт 8080)
│   │   ├── src/main/java/com/example/backend/
│   │   │   ├── game/             # Игровое ядро (Board, Piece, GameState, MoveGenerator...)
│   │   │   ├── game/ai/          # ИИ: минимакс, оценка, транспозиционная таблица
│   │   │   ├── session/          # Реестр партий, настройки
│   │   │   ├── service/          | GameService, LLM-комментарии, реестр
│   │   │   ├── controller/       | REST контроллеры
│   │   │   ├── dto/game/         # DTO records (StartGameRequest, MoveRequest, ...)
│   │   │   ├── model/            # JPA сущности (Tournament, LlmCommentEntity)
│   │   │   ├── repository/       # JPA репозитории
│   │   │   ├── config/           # Конфигурация Spring
│   │   │   └── error/            # Единый формат ошибок
│   │   ├── src/main/resources/
│   │   │   ├── application.yml
│   │   │   └── db/migration/     # Flyway миграции V1–V6
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── llm-service/              # Прокси к Ollama (порт 8081)
│   │   ├── src/main/java/com/example/llmservice/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── pom.xml                   # Aggregator (multi-module)
│   └── mvnw / mvnw.cmd           # Maven Wrapper
│
├── frontend/                     # React + TypeScript + Vite
│   ├── src/
│   │   ├── api/                  # API-клиент (fetch) и типы
│   │   ├── components/game/      # React компоненты (Board, Cell, StatusBar, ...)
│   │   ├── App.tsx               # Главный компонент
│   │   └── main.tsx
│   ├── Dockerfile
│   ├── nginx.conf                # Прокси /api → backend
│   ├── package.json
│   └── vite.config.ts
│
├── docker-compose.yml            # Все сервисы в одном compose
├── game-chashki-TZ.md            # Техническое задание
├── AGENTS.md                     # Правила проекта
├── docs/memory/                  # Артефактная памяти (STATE, FEATURES, DECISIONS, ...)
├── README.md                     # Этот файл
└── .gitignore
```

---

## 📊 Критерии приёмки (из ТЗ)

Все критерии раздела 13 ТЗ проверены и выполнены:

| Критерий | Статус |
|----------|--------|
| Правила русских шашек (взятие, большинство, серийные, превращение, ничья) | ✅ |
| Выбор цвета игрока; ИИ играет противоположным | ✅ |
| Настройка сложности и модели LLM в UI | ✅ |
| Ход ИИ в рамках бюджета времени | ✅ |
| Комментарий LLM со стилем, без подсказок, fallback | ✅ |
| Единый формат ошибок, защита от гонок | ✅ |
| Персистентность результата в `tournament` | ✅ |
| Отзывчивый UI, анимации, без визуальных ошибок | ✅ |
| Запуск через `docker compose up -d` | ✅ |

---

## 🔗 Полезные ссылки

| Ресурс | Ссылка |
|--------|--------|
| Техническое задание | `game-chashki-TZ.md` |
| Правила проекта | `AGENTS.md` |
| Артефактная память | `docs/memory/` |
| Состояние разработки | `docs/memory/STATE.md` |
| Журнал фич | `docs/memory/FEATURES.md` |
| Решения (ADR) | `docs/memory/DECISIONS.md` |
| Руководство запуска | `docs/memory/RUNBOOK.md` |
| Бэклог | `docs/memory/BACKLOG.md` |

---

## 🤝 Вклад в проект

1. Сделайтеフォк репозитория
2. Создайте ветку для вашей функции: `git checkout -b feature/your-feature`
3. Внесите изменения и пройдите тесты: `./mvnw test` и `npm test`
4. Создайте коммит в формате Conventional Commits (на русском):  
   `feat(backend): описание изменения`
5. Отправьте Pull Request

---

## 📄 Лицензия

Распространяется под лицензией MIT. См. файл `LICENSE` для подробностей.

---

> **Примечание:** Для работы комментариев ИИ необходимо запустить Ollama на хост-машине и загрузить модель `qwen2.5:7b-instruct` (или любую другую из allowlist). Без Ollama игра работает полностью, но комментарии будут детерминированными (fallback).

---

Разработано с ❤️ на Java 21, Spring Boot, React и Ollama.