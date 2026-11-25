# Catzilla

Catzilla helps people look after their cats together: health, habits, weight and feeding times, shared by everyone in a Home, including a temporary cat sitter. See the [product vision](docs/product-vision.md).

It starts as a phone-first web app on the home network and will grow into iOS and Android apps.

| Folder | Contents |
|---|---|
| [`mobile/`](mobile/) | Expo + React Native + TypeScript app |
| [`api/`](api/) | Spring Boot (Java 25) API, served under `/api` |
| [`contracts/`](contracts/) | `openapi.yaml`, the API contract generated from `api/` |
| [`infra/`](infra/) | Home-network deployment: Docker Compose, Caddy (HTTPS), PostgreSQL |
| [`docs/`](docs/) | [Product vision](docs/product-vision.md) and architecture decision records (`docs/adr/`) |

## Prerequisites

- Node 24 and npm, for `mobile/`
- A JDK 17 or newer to run Gradle, for `api/`. Gradle downloads JDK 25 for the build if it is not installed
- Docker, for API integration tests, `bootTestRun` and the deployment

## Local development

1. Start PostgreSQL only: `docker compose up -d db` in `infra/` (needs `infra/.env`).
2. Copy `api/.env.example` to `api/.env`, set `DATABASE_PASSWORD`, and run the API (`./gradlew bootRun` in `api/`, or `CatzillaApiApplication` from the IDE with `api/` as the working directory).
3. Set `EXPO_PUBLIC_API_URL=http://localhost:8080` in `mobile/.env` and run `npm run web` in `mobile/`.

`CORS_ALLOWED_ORIGINS` in `api/.env` lets the Expo web dev server (`localhost:8081`) call the API on `localhost:8080`. The deployed stack in `infra/` serves both from one origin and doesn't set it.

## Workflow

- Branch from `main`, open a PR, and squash-merge once `ci-ok` passes.
- Commit messages use [Conventional Commits](https://www.conventionalcommits.org/), scoped by project: `feat(mobile): …`, `fix(api): …`.
- An API change and its regenerated `contracts/openapi.yaml` land in the same PR.

Architecture decisions: [ADR-001](docs/adr/ADR-001-platform-framework.md) (Expo), [ADR-002](docs/adr/ADR-002-repository-and-backend.md) (monorepo and backend), [ADR-003](docs/adr/ADR-003-multi-tenancy-users-and-roles.md) (multi-tenancy, users and roles).
