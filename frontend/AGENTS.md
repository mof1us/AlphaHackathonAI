<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

## Container deployment

Dockerfile builds the pnpm-locked project with Node.js 24 and runs Next's standalone
server as the node user. Preserve standalone output and copy both public and
.next/static into the runtime image. Root docker-compose.yaml owns the service
network and startup dependencies. NEXT_PUBLIC_* values are build arguments,
whereas BACKEND_INTERNAL_URL and AUTHORIZATION_INTERNAL_URL are runtime server
variables. Current application pages do not implement API or OAuth integration.
Do not copy .env, node_modules, or local .next output into the build context.
Validate with pnpm build/pnpm lint and docker compose config --quiet.
