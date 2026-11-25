# ADR-004: Authentication with email and password, and Catzilla-issued JWTs

- **Status:** Accepted
- **Date:** 2026-11-26

## Context

[ADR-003](ADR-003-multi-tenancy-users-and-roles.md) requires every request to carry an **authenticated user** that the backend can trust, and it must be in place before the first Home endpoint. The product needs:

- **Now:** sign-up and sign-in with **email and password**.
- **Later:** sign-in with **Google** and **Apple**, for the same users.
- Clients: the web app on a phone (served by Caddy, same origin as the API), and later the native iOS and Android apps.

We need to decide how passwords are stored, what a signed-in client sends with each request, how the backend checks it, and how social sign-in fits in later without changing the rest.

## Decision

### 1. Passwords: Argon2id through Spring Security's password encoder

- Passwords are hashed with **Argon2id** using Spring Security's `DelegatingPasswordEncoder`. Each hash is stored as one string, for example `{argon2}$argon2id$v=19$m=…,t=…,p=…$<salt>$<hash>`.
- **Salting is built in:** the encoder generates a fresh random salt for every password and stores it inside that string. There's no separate salt column, and the salt doesn't need to be secret.
- The `{argon2}` prefix records the algorithm, so its parameters (or the algorithm itself) can change later. On a successful sign-in, a hash with outdated parameters is upgraded transparently.
- **Password rules** (following NIST SP 800-63B): at least 10 characters, at most 128, no composition rules (no "must contain a symbol"). Common and breached passwords may be rejected later.
- **Emails** are trimmed and lower-cased before they're stored or compared, and are unique.

### 2. Two tokens: a short-lived JWT access token and a long-lived refresh token

| Token | Format | Lifetime | Sent as | Stored by the backend |
|---|---|---|---|---|
| **Access token** | JWT, signed by the API | 15 minutes | `Authorization: Bearer <token>` on every API request | No; it is verified by its signature alone |
| **Refresh token** | 256-bit random value, opaque | 30 days, renewed on each use | Only to `POST /api/v1/auth/refresh` | Yes, as a SHA-256 hash, so it can be revoked |

A short access token keeps a stolen token useful for minutes only, and the backend can verify it without a database lookup. The refresh token provides a long session and can be revoked (sign-out, password change), which a JWT alone can't.

### 3. The access token's content

The API issues and verifies its own tokens; there's no external identity server.

Header and claims:

```json
{ "alg": "RS256", "typ": "JWT", "kid": "2026-11" }
```

```json
{
  "iss": "catzilla-api",
  "aud": "catzilla-api",
  "sub": "0b7c3f9e-5a1d-4c62-9a0e-6f1d2b8e4c37",
  "iat": 1795600000,
  "exp": 1795600900,
  "jti": "c1a5e0d2-…"
}
```

- **`sub` is the user's UUID** and is the only identity the backend uses: it's the "authenticated user" of ADR-003.
- **No Homes or roles in the token.** ADR-003 checks the membership for the Home in the path on every request, so access changes take effect immediately instead of when a token expires.
- **No personal data** (email, name) in the token. JWTs are signed, not encrypted: anyone holding one can read its content.

### 4. Signing: RS256 with a key pair from configuration

- Tokens are signed with **RS256** (an RSA 2048-bit private key) and verified with the matching public key.
- The private key is supplied by configuration (`JWT_PRIVATE_KEY`, PEM), like the database password: in `infra/.env` for the deployment and in `api/.env` for local development. It's never committed.
- The `kid` (key ID) header names the key that signed a token, so keys can be rotated: a new key signs new tokens while the previous public key keeps verifying until its last tokens expire.
- In tests and local development without a configured key, the API generates a temporary key at startup and logs a warning; tokens then stop working after a restart.

An asymmetric key is chosen over a shared secret (HS256) because only the API ever needs the private key: anything else that verifies tokens later (a second service, a gateway) gets just the public key, and a key leaked from a verifier can't be used to create tokens.

### 5. Implementation with Spring Security

- `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server`, configured as a **resource server with JWT**. Spring checks the signature, `iss`, `aud` and `exp` (with 60 seconds of clock skew) on every request, and exposes the result as the authenticated principal.
- Tokens are created with Spring Security's `JwtEncoder` (Nimbus JOSE + JWT). No hand-written JWT code.
- The API is **stateless**: no server sessions, no session cookies for the access token. CSRF protection is unnecessary for Bearer-token requests.
- A small helper resolves the current user's ID from `sub`; ADR-003's Home check builds on it.

### 6. Endpoints

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/auth/register` | Create a user (email, password, display name) and sign them in |
| `POST /api/v1/auth/login` | Check email and password; return an access token and a refresh token |
| `POST /api/v1/auth/refresh` | Exchange a valid refresh token for a new pair; the old refresh token stops working |
| `POST /api/v1/auth/logout` | Revoke the current refresh token |
| `GET /api/v1/me` | The signed-in user's profile |

Everything else requires a valid access token.

- A failed sign-in always answers **"Invalid email or password"**, whether the email exists or not.
- Sign-in attempts are **rate-limited** per email and per IP address to slow down password guessing.
- **Refresh token reuse is detected:** if a refresh token that was already exchanged is presented again, someone copied it, so all of that user's refresh tokens are revoked.

### 7. Storing tokens in the app

| Platform | Access token | Refresh token |
|---|---|---|
| Native (iOS, Android) | Memory | `expo-secure-store` (Keychain or Keystore) |
| Web | Memory | An `HttpOnly`, `Secure`, `SameSite=Strict` cookie limited to `/api/v1/auth`, set by the API |

On the web, JavaScript can't read an `HttpOnly` cookie, so a cross-site-scripting bug can't steal the long-lived token. The web app and the API share one origin behind Caddy, which keeps the cookie simple. The difference stays inside one platform-specific module of the app (`*.web.ts` / `*.native.ts`).

When a request returns `401`, the app refreshes once and retries; if refreshing fails, it signs the user out.

### 8. Data model

```
users                (id uuid, email, email_verified, display_name, created_at)
password_credentials (user_id, password_hash, updated_at)
refresh_tokens       (id uuid, user_id, token_hash, family_id, expires_at, used_at, revoked_at, created_at)
user_identities      (user_id, provider, provider_subject, created_at)   -- added with social login
```

Passwords live outside `users`, so a user who signs in only with Google or Apple simply has no password row.

### 9. Social sign-in later (Google, Apple)

Google and Apple sign-in will be **added to this design, not replace it**:

1. The app signs in with Google or Apple on the device and receives that provider's **ID token**.
2. It sends that ID token to the API (`POST /api/v1/auth/google`, `/apple`).
3. The API verifies it against the provider's published keys, finds or creates the user through `user_identities (provider, provider_subject)`, and returns **the same Catzilla tokens** as a password sign-in.

So the rest of the API only ever sees Catzilla's own JWTs. How accounts with the same email are linked will be decided then (it requires the provider to have verified that email).

## Rationale

- **Argon2id** is the current OWASP recommendation for password hashing; Spring Security supports it, salts per password, and makes upgrading parameters routine.
- **Short JWT + revocable refresh token** combines cheap stateless verification on every request with the ability to sign users out.
- **Spring Security's resource-server support** does the verification correctly (signature, expiry, issuer, audience), so the project writes no cryptographic code.
- **Identity only in the token, permissions from the database** matches ADR-003 and keeps tokens small and stable.

## Consequences

- New dependencies: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, and Bouncy Castle (`bcprov`), which Spring Security's Argon2 encoder needs.
- Once security is added, every endpoint requires authentication unless listed as public. Health and API docs (`/api/actuator/health`, `/api/v3/api-docs`) must be explicitly made public; the app's health badge depends on it.
- `JWT_PRIVATE_KEY` becomes a required secret in `infra/.env` and `api/.env.example`, with a documented command to generate one.
- Integration tests need a helper that signs in (or mints a token with the test key) to call protected endpoints.
- **Not covered yet, needed before anyone outside the home uses the app:** email verification, password reset by email, and changing email or password. Each needs an email provider and gets its own change.
- Rate limiting needs a store. An in-memory limiter is enough while the API runs as a single instance.

## Alternatives considered

### Server-side sessions with a cookie

Simple and revocable, but native apps handle cookies awkwardly, and every request needs a session lookup. Rejected in favour of Bearer tokens that work the same on web and native.

### Long-lived JWT without refresh tokens

Simplest, but a stolen token stays valid until it expires and sign-out can't really end a session. Rejected.

### HS256 (shared secret) instead of RS256

Fewer moving parts, but any service that verifies tokens could also forge them. With a single API the practical difference is small; RS256 costs little extra and keeps the option open. Acceptable fallback if key handling becomes a burden.

### External identity provider (Keycloak, Auth0, Firebase Auth, Cognito)

Provides sign-in, social login, email flows and MFA out of the box, at the cost of another service to run or pay for, and a dependency for a home-network MVP. Rejected for now; revisit if account management (email flows, MFA, admin) grows beyond what's reasonable to maintain.

### bcrypt instead of Argon2id

Still acceptable and needs no extra library. Argon2id is preferred for its resistance to GPU cracking; switching is a configuration change thanks to the `{id}` prefix.

## Revisit this decision if

- Another service needs to authenticate users, or the API runs as several instances (rate limiting and key distribution change).
- Account features grow to email flows, MFA and administration: compare again with an external identity provider.
- A platform's token storage guidance changes (for example, web refresh cookies become blocked).
