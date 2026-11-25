# catzilla-api

The Catzilla backend: users, Homes and their members, cats, feeding schedules and logs (see the [product vision](../docs/product-vision.md)).

Spring Boot 4.2 service on Java 25, with PostgreSQL and Flyway. Every endpoint is served under `/api`.

## Run

```bash
./gradlew bootTestRun   # starts PostgreSQL in Docker automatically (needs Docker)
```

Health check: <http://localhost:8080/api/actuator/health>. OpenAPI: <http://localhost:8080/api/v3/api-docs>.

To use an existing database instead, set `DATABASE_URL`, `DATABASE_USERNAME` and `DATABASE_PASSWORD` and run `./gradlew bootRun`.

## Authentication

Email and password sign-in with JWT access tokens and rotating refresh tokens ([ADR-004](../docs/adr/ADR-004-authentication.md)).

| Endpoint | Auth | Purpose |
|---|---|---|
| `POST /api/v1/auth/register` | public | Create an account (`email`, `password` of 10–128 characters, `displayName`) and sign in. 409 if the email exists |
| `POST /api/v1/auth/login` | public | Sign in. 401 "Invalid email or password."; 429 with `Retry-After` after 5 failures per email or 20 per IP in 15 minutes |
| `POST /api/v1/auth/refresh` | refresh token | New access and refresh token; the old refresh token can't be reused (reuse signs the user out everywhere) |
| `POST /api/v1/auth/logout` | refresh token | Ends that session; always 204 |
| `GET /api/v1/me` | access token | The signed-in user's profile |

- Send the access token as `Authorization: Bearer <accessToken>`. It lasts 15 minutes; refresh tokens last 30 days.
- `register` and `login` accept `"tokenDelivery": "COOKIE"` (web app): the refresh token is then set as an `HttpOnly` cookie limited to `/api/v1/auth` instead of being returned in the body. `refresh` and `logout` read it from the body (`refreshToken`) or that cookie.
- Errors are [problem details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`).
- Without `JWT_PRIVATE_KEY` the API signs with a temporary key and logs a warning; see `.env.example`.

## Checks

```bash
./gradlew build           # format check, Error Prone, tests, coverage (build/reports/jacoco)
./gradlew spotlessApply   # fix formatting
./gradlew updateContract  # regenerate ../contracts/openapi.yaml after API changes
```

See [AGENTS.md](AGENTS.md) for conventions.
