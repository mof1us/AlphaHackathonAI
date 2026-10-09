# Решение хакатона

`elaboration/` содержит проработки продукта и [архитектуру](elaboration/architechure/README.md),
`frontend/` — Next.js, `backend/` — модульный Kotlin/Spring backend.

## Полный стек

Из корня монорепозитория:

```sh
cp -n .env-example .env
docker compose up --build -d
docker compose ps
```

| Сервис | Адрес по умолчанию |
| --- | --- |
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Сервер авторизации | http://localhost:9000 |
| Temporal gRPC | localhost:7233 |
| Temporal UI | http://localhost:8081 |
| PostgreSQL | localhost:5432 |
| Temporal runner | Фоновый процесс без HTTP |

Compose собирает frontend и три backend-приложения из исходников, ждёт готовности
PostgreSQL и namespace Temporal, затем запускает workers и HTTP-приложения.
`backend-api` и `authorization-server` проверяются через `/actuator/health`.
Frontend ждёт готовности обоих HTTP-приложений.
Temporal UI подключается к `temporal:7233` внутри Docker-сети и запускается после
готовности Temporal. В нём доступны namespaces, workflow, история выполнения и расписания.
Внешний порт UI задаётся через `TEMPORAL_UI_PORT` (по умолчанию 8081).

Все backend-модули используют одну прикладную БД `elephants`. Temporal хранит свою
историю в служебных БД `temporal` и `temporal_visibility` в том же PostgreSQL-контейнере;
это не разделение прикладных DAO по базам. Temporal создаёт свои БД и схему при первом
старте. Прикладных миграций пока нет, приложения не применяют их автоматически.
Данные сохраняются в именованном томе `postgres-data`.

Порты, пароль БД и namespace задаются в корневом `.env` по [.env-example](.env-example).
Порты публикуются на хосте. При запуске на сервере используйте его адрес и задайте
соответствующие внешние URL frontend.
`NEXT_PUBLIC_API_URL` и `NEXT_PUBLIC_AUTHORIZATION_URL` передаются при сборке Next.js:
после их изменения пересоберите frontend. Серверный код frontend получает
`BACKEND_INTERNAL_URL=http://backend-api:8080` и
`AUTHORIZATION_INTERNAL_URL=http://authorization-server:9000`.
Прикладные HTTP-вызовы frontend и OAuth-поток пока не реализованы.

```sh
docker compose logs -f backend-api temporal-runner
docker compose down
```

`down` сохраняет данные. `docker compose down -v` удаляет том с прикладной БД и историей Temporal.

Сборка требует доступа к Docker Hub, Maven/Gradle, npm и Google Fonts, используемым текущим frontend.
Образ Temporal и его PostgreSQL-настройки основаны на [официальном Compose-примере](https://github.com/temporalio/docker-compose/blob/main/docker-compose-postgres.yml).

## Разработка

- [Backend: модули, jOOQ, миграции и Temporal](backend/README.md)
- [Frontend: локальный запуск и контейнер](frontend/README.md)

Backend: JDK 21 и `cd backend && ./gradlew build`.
Frontend: Node.js 24 и `cd frontend && pnpm install --frozen-lockfile && pnpm build`.
Планируемый CI в `.github/`: тесты, сборка образов и публикация в Docker Hub;
перезапуск на сервере выполняется вручную.
