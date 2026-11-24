# catzilla-api

Spring Boot 4.2 service on Java 25, with PostgreSQL and Flyway. Every endpoint is served under `/api`.

## Run

```bash
./gradlew bootTestRun   # starts PostgreSQL in Docker automatically (needs Docker)
```

Health check: <http://localhost:8080/api/actuator/health>. OpenAPI: <http://localhost:8080/api/v3/api-docs>.

To use an existing database instead, set `DATABASE_URL`, `DATABASE_USERNAME` and `DATABASE_PASSWORD` and run `./gradlew bootRun`.

## Checks

```bash
./gradlew build           # format check, Error Prone, tests, coverage (build/reports/jacoco)
./gradlew spotlessApply   # fix formatting
./gradlew updateContract  # regenerate ../contracts/openapi.yaml after API changes
```

See [AGENTS.md](AGENTS.md) for conventions.
