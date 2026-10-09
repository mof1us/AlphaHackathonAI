# Backend

Kotlin 2.3.21, Java 21 и Spring Boot 4.1.1. Gradle-корень находится в `backend/`.

| Модуль | Ответственность |
| --- | --- |
| `utils` | Контекст пользователя, параметры фильтрации, сортировки и пагинации |
| `domain` | Доменные сервисы, интерфейсы DAO/репозиториев, границы транзакций |
| `data` | Единая PostgreSQL, R2DBC, jOOQ, генерация схемы и Flyway |
| `authorization` | Общие компоненты авторизации и кодирование паролей |
| `integrations` | Клиент Temporal, suspend-адаптеры, workflow и activity |
| `server` | Реактивное HTTP API, порт 8080 |
| `authorization-server` | Отдельное servlet-приложение для авторизации, порт 9000 |
| `temporal-runner` | Отдельный процесс workers и расписаний, без HTTP |

`domain` зависит от `utils`; `data` и `integrations` зависят от `domain`, но не друг от друга.
Исполняемые приложения собирают нужные модули. API не сканирует runner и сервер авторизации.
Сейчас `authorization` и `authorization-server` — каркас: пользователи, OAuth-клиенты,
постоянное хранение токенов и прикладные правила доступа ещё не реализованы.
Обвязка логирования МТИ не переносилась; используются стандартные логи библиотек.

## Сборка и запуск

Из этой директории, с JDK 21 в `JAVA_HOME`:

```sh
./gradlew build
./gradlew :server:bootRun
./gradlew :authorization-server:bootRun
./gradlew :temporal-runner:bootRun
```

Последние три команды запускают независимые процессы. Полный стек, включая frontend,
поднимается через `docker compose up --build -d` из корня монорепозитория.
JAR-файлы: `server/build/libs/server.jar`,
`authorization-server/build/libs/authorization-server.jar`,
`temporal-runner/build/libs/temporal-runner.jar`.

Для запуска через Gradle скопируйте `.env.example` в `backend/.env` и задайте значения.
Приложения и задачи БД читают один набор `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
Реальное окружение имеет приоритет. `bootRun` работает из `backend/`, поэтому относительный
импорт `.env` одинаков для всех трёх приложений. В контейнеры значения передаёт Compose.

## jOOQ и транзакции

Один `JooqScope`, один `DSLContext`, один R2DBC-пул и один `ReactiveTransactionManager`.
JDBC подключён к той же базе для инфраструктурных потребителей; запросы DAO выполняются
через R2DBC. Пулы ленивые: создание Spring-контекста не требует доступной БД.

DAO внедряет `JooqScope` и вызывает `scope.use { ... }`. Внутри доступны
`selectFrom`, `insertInto`, `update`, `deleteFrom`, `awaitList`, `awaitOne`,
`awaitRowsUpdated` и `fetchCount`. `awaitOne` возвращает `null` при отсутствии строк
и отклоняет несколько строк. `use` возвращает результат блока и сам не открывает транзакцию.
Не используйте блокирующие `fetch()` и `execute()` для выполнения запросов DAO.

Для нескольких запросов используйте `ITransactionManager.transaction { ... }`.
`SpringTransactionManager` выполняет блок через `TransactionalOperator.executeAndAwait`;
ошибка и отмена корутины откатывают транзакцию. `JooqConnectionFactory` сохраняет
соединение за Spring до завершения транзакции, а `CoreSubscriberProvider` переносит Reactor Context.

Общие фильтры и сортировки валидируются в `utils.params`, условия `inIfNotNull` /
`notInIfNotNull` и `withPagination` находятся в `data.config.helpers`.
Сервисы обращаются к репозиториям, репозитории — к DAO; SQL и маппинг остаются в `data`.

## Миграции и генерация

```sh
./gradlew :data:migrate
./gradlew :data:generateJooq
```

Это **явные** операции над выбранной базой. Сборка, тесты и запуск приложений не применяют
миграции и не запускают генерацию автоматически. SQL хранится в
`data/src/main/resources/migrations`, Kotlin-код jOOQ — в
`data/src/main/kotlin/ru/alfahack/elephants/backend/data/jooq`.
Сгенерированные файлы нужно коммитить после изменения схемы; вручную их не редактируют.
Бизнес-схема ещё не задана, поэтому миграций и сгенерированных таблиц пока нет.

## Temporal

Настройки: `INTEGRATIONS_TEMPORAL_TARGET` (по умолчанию `127.0.0.1:7233`) и
`INTEGRATIONS_TEMPORAL_NAMESPACE` (`default`). Общие клиенты создаются без обращения к серверу.
Только `temporal-runner` регистрирует и запускает workers. `TEMPORAL_WORKERS_ENABLED=false`
отключает polling и согласование расписаний для локальной проверки Spring-контекста.

- Очередь добавляется в `TemporalTaskQueue`, реализация workflow и Spring activity —
  в `TemporalWorkersFactory`, интерфейс workflow — в `TemporalWorkflowTypes`.
- Конвертер Temporal использует Jackson 2 с Kotlin module независимо от HTTP Jackson 3.
- Suspend-activity вызывается через `newSuspendActivityStub`. По умолчанию одна попытка;
  явные `RetryOptions` сохраняются. Реальная приостановка завершается через local manual completion.
- Workflow должен оставаться детерминированным: только API Temporal и вызовы activity.
  `delay`, обычные dispatchers, I/O и произвольные приостановки допустимы в activity, не в workflow.
- `UserContext.propagated()` переносит пользователя и безопасные метаданные сессии.
  `withActivityContext` убирает HTTP-пользователя перед входом в фоновую доменную операцию.
- Расписываемый workflow наследует `WorkflowScheduler`, реализует ровно один
  `@WorkflowInterface`, имеет открытый конструктор без аргументов и регистрируется Spring-бином.
  Конструктор не вызывает API Temporal. Bootstrap создаёт/обновляет только объявленные
  расписания, сохраняет pause-состояние и не удаляет чужие или больше не объявленные расписания.

Проверочный workflow: `HelloWorldWorkflow`, очередь `HELLO_WORLD_QUEUE`, вход `{"name":"Elephants"}`.
Проверка запущенного Compose-стека из корня:

```sh
docker compose exec temporal temporal workflow execute --address temporal:7233 \
  --namespace default --type HelloWorldWorkflow --task-queue HELLO_WORLD_QUEUE \
  --input '{"name":"Elephants"}'
```

Тесты запускают встроенный Temporal и подменяют только R2DBC-драйвер, сохраняя настоящий
jOOQ и Spring-транзакции. Внешние PostgreSQL/Temporal и применение миграций им не нужны.
