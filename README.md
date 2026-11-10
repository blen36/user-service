# User Service

По умолчанию сервис слушает 8081. Для Docker Desktop скопируй `.env.example` в `.env`, укажи пароль PostgreSQL и общий с Auth секрет регистрации. Создай сеть `java-advanced-services` один раз для двух репозиториев и запусти `docker compose up -d --build`. В Compose адрес Auth — `http://auth-service:8082`. Для запуска из IntelliJ используй `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `AUTH_REGISTRATION_KEY` и `AUTH_JWKS_URI=http://localhost:8082/.well-known/jwks.json`. Локальному профилю нужны доступные PostgreSQL и Redis.

## Проверка HTTP

| Запрос | Что проверяет |
|---|---|
| `GET /api/users?name=iv&surname=pet&page=0&size=5` | Администратор получает пользователей, удовлетворяющих обоим фильтрам. Каждый фильтр опционален |
| `GET /api/cards?name=iv&surname=pet` | Список карт с фильтром по владельцу для администратора |
| `POST /api/users/{userId}/cards` | Создание активной карты. Шестая активная карта даёт 409 |
| `PATCH /api/cards/{id}/status` | JSON `{"active":false}` освобождает место. Повторная активация проверяет тот же лимит |
| `PATCH /api/users/{id}/status` | Администратор меняет статус пользователя |
| `GET /api/users/{id}` | Владелец или администратор читает профиль с картами. Профиль кэшируется в Redis |
| `POST /api/users` и `DELETE /api/users/{id}` | Регистрационный сервис предъявляет внутренний ключ. Администратору доступен обычный Bearer JWT |

Миграция 003 добавляет уникальный индекс на `lower(email)`, чтобы одновременные запросы с разным регистром тоже соблюдали уникальность. Если база содержит старые дубликаты, запрос ниже покажет группы для ручной сверки перед миграцией. Миграция не удаляет записи.

```sql
SELECT lower(email), array_agg(id ORDER BY id)
FROM users
GROUP BY lower(email)
HAVING count(*) > 1;
```

Под Java 21 с запущенным Docker Desktop выполни `.\mvnw.cmd clean verify`. В тестах PostgreSQL общий для классов, а сценарии кэша дополнительно используют Redis. Проверка числа SQL-запросов использует JWT администратора. Для обычного пользователя сервис отдельно проверяет активность аккаунта в БД при каждом запросе, в том числе когда профиль уже в кэше.
