# Журнал фич

Обновляется после каждой реализованной фичи (часть Definition of Done).

| ID | Спринт | Название | Статус | Файлы | Тесты / DoD | Коммит | Дата |
|----|--------|----------|--------|-------|-------------|--------|------|
| S0.1 | Sprint 0 | Очистка backend: удалены дубли вне `src`, `application.properties` слит в `application.yml` | Done | `backend/config/CordConfig.java` (удалён), `backend/controller/ItemController.java` (удалён), `backend/dto/CreateItemRequest.java` (удалён), `backend/dto/ItemDto.java` (удалён), `backend/repository/ItemRepository.java` (удалён), `backend/src/main/resources/application.properties` (удалён), `backend/src/main/resources/application.yml` | `./mvnw -DskipTests package` — OK; запуск с БД: Flyway `V1__create_items.sql` применён (`flyway_schema_history`, таблица `items`), `GET /api/items` → 200, `POST /api/items` → 201 | `2d6ce68` | 2026-10-18 |

**Статусы:** `Todo` | `In progress` | `Done` | `Blocked`.
