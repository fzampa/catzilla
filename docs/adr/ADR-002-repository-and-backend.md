# ADR-002: Monorepo with an Expo app and a Spring Boot API

- **Status:** Accepted
- **Date:** 2026-11-24

## Context

Catzilla is restarting as a green-field project with a separate frontend and backend. The frontend follows [ADR-001](ADR-001-platform-framework.md) (Expo, React Native, TypeScript). The backend will be a Java service built with Spring Boot. A single developer or a small team owns both parts, and most features change both the API and the screens that use it.

We need to decide how to organise the code in git, how the two parts agree on the API, and the baseline tooling for builds, checks and deploys.

## Decision

1. **One git repository, `catzilla`**, with top-level folders named for what they hold:
   - `mobile/`: the Expo app (package `catzilla-mobile`).
   - `api/`: the Spring Boot service (Gradle project `catzilla-api`, base package `app.catzilla.api`).
   - `contracts/openapi.yaml`: the HTTP API contract, generated from the API and committed.
   - `infra/`: the home-network deployment (Docker Compose, Caddy).
   - `docs/adr/`: decisions that span the repository.
2. **Each project keeps its own toolchain.** There's no root `package.json` or JS workspace: npm runs in `mobile/` and the Gradle wrapper in `api/`. Bring in workspaces only when a second JS package exists.
3. **Backend baseline:** Java 25 (LTS) via a Gradle toolchain (the foojay resolver downloads it when it is missing), Spring Boot 4.2, Gradle Kotlin DSL with a version catalog, PostgreSQL with Flyway migrations, springdoc-openapi, Spotless (Palantir Java Format), Error Prone, JaCoCo, ArchUnit and Testcontainers. Code is packaged by feature (`app.catzilla.api.<feature>`).
4. **The API contract is enforced in CI.** `OpenApiContractTest` fails when the committed `contracts/openapi.yaml` differs from what the code serves; `./gradlew updateContract` regenerates it. An API change and its spec update land in the same PR.
5. **The API is served under `/api`** (servlet context path). In deployment, one HTTPS origin serves both the web build and `/api`, so the browser MVP needs no CORS setup.
6. **CI is path-filtered GitHub Actions** with one aggregate `ci-ok` job to use as the required status check. CodeQL, gitleaks and dependency review run in a separate security workflow.
7. **Trunk-based workflow:** short-lived branches, PRs into a protected `main`, squash merges, and Conventional Commits scoped by project (`feat(mobile):`, `fix(api):`). `mobile` and `api` are versioned independently.

## Rationale

- A cross-cutting change (API plus UI) is one PR and one review, and contract drift is caught before merge.
- With a single owner, one repository means one place for branch protection, issues, ADRs and agent guidance.
- Path filters keep CI cost close to a multi-repo setup.
- Java 25 is the current LTS and is supported by Spring Boot 4.2. Pinning it as a Gradle toolchain keeps local, CI and Docker builds on the same JDK, whichever JDK runs Gradle itself.
- Building on PostgreSQL from the start, with Testcontainers in tests, keeps local, CI and the home deployment on the same database.

## Consequences

- Contributors need Node (for `mobile/`) and a JDK (for `api/`). Docker is needed for integration tests and the deployment; those tests are skipped when Docker is unavailable, and always run in CI.
- `app.catzilla` is a placeholder namespace. Replace it with a domain the project controls before the first store release: the Java group/package now, and the iOS bundle identifier and Android package when EAS is set up.
- Tags must carry a project prefix (`mobile-v0.2.0`, `api-v0.3.0`) because the two parts release independently.

## Alternatives considered

### Separate repositories for frontend and backend

This gives independent access control and history per part. It's worth doing when different teams own the parts or their release processes diverge sharply. For now it would mean two PRs per feature and no CI check that the client matches the server. A part can be split out later, with its history, using `git filter-repo --subdirectory-filter <folder>`.

### Root JS workspaces (npm, pnpm, Nx, Turborepo)

There's only one JS project, so this adds tooling with no benefit. Revisit when shared TypeScript packages or a second app appear.

### Maven instead of Gradle

Also a sound choice. Gradle was chosen for the version catalog, faster incremental builds and simpler custom tasks (such as `updateContract`).

## Revisit this decision if

- Separate teams take ownership of the mobile app and the API.
- A second deployable or a shared TypeScript package is added (then consider workspaces).
- A newer Java LTS is released and supported by Spring Boot: raise the toolchain, the CI `java-version` and the Docker base images together.
