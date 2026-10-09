# Frontend

Next.js 16, React 19, TypeScript и pnpm 11.28.5. Для контейнера используется Node.js 24.

```sh
pnpm install --frozen-lockfile
pnpm dev
pnpm build
pnpm lint
```

Приложение доступно на http://localhost:3000. Конфигурация `output: "standalone"`
создаёт автономный сервер для Docker; образ также включает `public` и `.next/static`.
Полный стек: `docker compose up --build -d` из корня монорепозитория.

Compose передаёт серверные адреса `BACKEND_INTERNAL_URL` и `AUTHORIZATION_INTERNAL_URL`.
Для будущих клиентских запросов предусмотрены аргументы сборки `NEXT_PUBLIC_API_URL`
и `NEXT_PUBLIC_AUTHORIZATION_URL`. Их задают в корневом `.env` и меняют с пересборкой образа.
Прикладные обращения к API и авторизация пока не реализованы. Текущие Google Fonts
скачиваются во время сборки, поэтому ей нужен доступ к сети.
