# api/ — Spring Boot service

Java 25, Spring Boot 4.2, Gradle (Kotlin DSL). Dependency and plugin versions live in `gradle/libs.versions.toml`, except Spring-managed starters, which have no version.

## Commands

Run from `api/`:

```bash
./gradlew build             # spotlessCheck + Error Prone + all tests + JaCoCo report
./gradlew spotlessApply     # format Java and Gradle files
./gradlew updateContract    # regenerate ../contracts/openapi.yaml after an API change
./gradlew bootTestRun       # run locally with PostgreSQL in Docker (Testcontainers)
./gradlew bootRun           # run against DATABASE_URL / DATABASE_USERNAME / DATABASE_PASSWORD
```

## Conventions

- **Package by feature:** `app.catzilla.api.<feature>` holds that feature's controller, service, entities and repositories. Put cross-cutting code in `app.catzilla.api.shared`. `ArchitectureTest` forbids dependency cycles between feature packages.
- **Tenancy ([ADR-003](../docs/adr/ADR-003-multi-tenancy-users-and-roles.md)):** Home data lives under `/v1/homes/{homeId}/...` and every such request passes the membership check (no access: 404; wrong role: 403). Take the user only from the authenticated principal. Every Home table has `home_id`, and every Home query filters by it. Use UUIDs for IDs. Test each Home endpoint with a user from another Home.
- **HTTP:** everything is under the `/api` context path. Version public endpoints as `/v1/...` in controller mappings (`/api/v1/...` externally). Validate request bodies with Jakarta Validation.
- **Database:** schema changes only through new Flyway migrations in `src/main/resources/db/migration` (`V<n>__<description>.sql`). Never edit an applied migration. Hibernate runs with `ddl-auto: validate`.
- **Contract:** every change to the API surface needs `./gradlew updateContract` and the updated `contracts/openapi.yaml` in the same commit. `OpenApiContractTest` fails otherwise.
- **Tests:** unit tests need no Spring context where possible. Anything that touches the database uses Testcontainers PostgreSQL (`@Testcontainers(disabledWithoutDocker = true)`), never H2.
- **Static analysis:** Error Prone warnings fail the build for main code. Fix them rather than suppressing them; if a suppression is necessary, add `@SuppressWarnings("<Check>")` with a comment explaining why.
- **Configuration:** `application.yaml` reads secrets from environment variables, and from an optional, git-ignored `api/.env` (template: `.env.example`). Never commit real credentials.
- **CORS:** `shared/web/CorsFilterConfiguration` allows only the origins in `CORS_ALLOWED_ORIGINS` (empty by default). It exists for local development; the deployment is same-origin behind Caddy, so don't set it there or widen it to `*`.
