# temporal-runner

Самостоятельный процесс без HTTP. Регистрирует workers, запускает polling, затем создаёт/обновляет объявленные расписания. При остановке дожидается workers. `TEMPORAL_WORKERS_ENABLED=false` отключает эти действия.

Команды и общие правила — в [README backend](../README.md).
