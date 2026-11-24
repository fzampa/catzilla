# Catzilla — agent guidance

Catzilla is a monorepo with a mobile/web app and its API. See [ADR-002](docs/adr/ADR-002-repository-and-backend.md) for the repository layout and backend decisions.

| Folder | What it is | Guide |
|---|---|---|
| `mobile/` | Expo + React Native + TypeScript app ([ADR-001](docs/adr/ADR-001-platform-framework.md)) | [mobile/AGENTS.md](mobile/AGENTS.md) |
| `api/` | Spring Boot (Java) API, served under `/api` | [api/AGENTS.md](api/AGENTS.md) |
| `contracts/` | `openapi.yaml`, generated from `api/` and committed | — |
| `infra/` | Home-network deployment (Docker Compose + Caddy) | [infra/README.md](infra/README.md) |
| `docs/adr/` | Architecture decision records | — |

Read the guide for the folder you are changing before you edit it.

## Project direction

- The initial release is a phone-oriented mini-MVP opened in a browser over the home network. The intended evolution is an iOS and Android mobile app.
- A public, search-indexed website is not currently a requirement. Do not add SEO or public-site infrastructure without a product reason.

## Working principles

- Before changing files, inspect the existing project structure and follow its established conventions.
- Prefer small, focused changes. Keep the app running on web and native platforms.
- Keep each project's toolchain inside its folder. Do not add a root `package.json`, JS workspaces or a root build tool without an ADR.
- An API change and its `contracts/openapi.yaml` update go in the same change. Regenerate the spec with `./gradlew updateContract` from `api/`; never edit it by hand.
- Keep business rules and data access independent of UI where practical; avoid adding abstraction without a concrete reuse need.
- Preserve types. Avoid `any` in TypeScript unless an external boundary makes it necessary and the reason is documented.
- Do not add credentials, secrets, or private home-network details (IPs, host names) to source control. Use the `.env.example` files as templates.
- For architectural decisions that affect future work, add a numbered ADR under `docs/adr/` and link it from this file.
- Commit messages follow Conventional Commits, scoped by project: `feat(mobile): …`, `fix(api): …`, `chore(ci): …`, `docs(adr): …`.

## Validation

- Use the scripts and checks already defined by each project. Do not invent commands or claim a check passed unless it was run.
  - `mobile/`: `npm run check` (typecheck, lint, format, tests); `npm run build:web` for the web export.
  - `api/`: `./gradlew build` (format check, Error Prone, tests, coverage). Integration tests need Docker and are skipped without it.
- CI (`.github/workflows/ci.yml`) runs the same checks, filtered by path; `ci-ok` is the aggregate required check.
- When a change affects user-facing behavior, validate the relevant platform(s) when the required tooling is available. State any platform that could not be checked.
