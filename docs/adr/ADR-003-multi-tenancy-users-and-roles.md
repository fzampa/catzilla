# ADR-003: Multi-tenancy by Home, with users and per-Home roles

- **Status:** Accepted
- **Date:** 2026-11-25

## Context

Catzilla lets people look after their cats together (see the [product vision](../product-vision.md)). Cats and everything about them (profiles, feeding schedules, feeding logs, diary notes, notifications) belong to a **Home**. Several people manage a Home, and later a cat sitter can look after a Home's cats for a limited time.

The same person can take part in several Homes in different ways: a member of their own Home and of a relative's, and a cat sitter for friends. Data from one Home must never reach someone without a role in it.

We need one rule for how tenancy, users and roles are modelled, and how every backend request says which user and which Home it is about.

## Decision

### 1. The Home is the tenant

- Every piece of cat data belongs to exactly one Home. Tables holding Home data have a non-null `home_id`.
- All Homes share one database and one schema; `home_id` is the tenant key.
- A record never points at a record in another Home. For example, a feeding log's cat must be in the same Home as the log. The schema enforces this with composite foreign keys that include `home_id`.

### 2. Everyone is a user; roles belong to a user in a Home

- Everybody who uses the app is a **user**. There are no separate user types.
- A user's participation in a Home is a **membership** with a **role**:

  | Role | Meaning |
  |---|---|
  | `MEMBER` | Manages the Home: cats, schedules, invites, other people's access. Created by creating the Home or by accepting its invite link. |
  | `CAT_SITTER` | Looks after the Home's cats temporarily: reads cat profiles, health and diary, records feedings, writes notes. Valid only between `valid_from` and `valid_until`. Planned for a later release. |

- A user can be a `MEMBER` of several Homes and a `CAT_SITTER` for several Homes, both at the same time.
- A user has **at most one role per Home**.

Data model:

```
users             (id, ...)
homes             (id, name, time_zone, ...)
home_memberships  (user_id, home_id, role, valid_from, valid_until, created_at)
                   primary key (user_id, home_id)
```

`valid_from` and `valid_until` are timestamps (instants), required for `CAT_SITTER` and empty for `MEMBER`.

### 3. Every request identifies the user and the Home

Every request to the backend carries both identifiers, each in a place the backend can trust:

| Identifier | Where it travels | Why there |
|---|---|---|
| **User** | The authenticated identity of the request (the access token's subject). Never a header, query parameter or body field the client chooses. | The user ID must be proven, not claimed. If clients could send any user ID, anyone could act as anyone. |
| **Home** | The URL path: `/api/v1/homes/{homeId}/...`, for example `/api/v1/homes/{homeId}/cats/{catId}/feedings`. | It's explicit in every request, appears in the OpenAPI contract and logs, and makes it obvious which tenant an endpoint works on. |

The only endpoints without a `homeId` are the ones that come before or above a Home: registering and signing in, the current user's profile, listing the user's Homes (`GET /api/v1/homes`), creating a Home, and accepting an invite.

How the user signs in (sessions, tokens, an external identity provider) is a separate decision for a later ADR. This ADR only requires that each request has an authenticated user the backend can identify.

### 4. The backend checks access on every Home request

For every request under `/api/v1/homes/{homeId}/...`, before any business logic runs:

1. Resolve the authenticated user. No user means `401 Unauthorized`.
2. Load the user's membership for `{homeId}`.
3. If there's no membership, or it's a `CAT_SITTER` membership outside its validity period, respond `404 Not Found`. Returning 404 rather than 403 avoids revealing that the Home exists.
4. If the role doesn't allow the operation (a sitter editing a schedule, say), respond `403 Forbidden`.
5. Otherwise, run the operation with the user, Home and role available as a request-scoped **Home context**.

Repositories and queries always filter by the Home in that context. A query for Home data without a `home_id` condition is a bug. Accessing a child resource also checks that it belongs to the Home in the path: `/homes/A/cats/{catId}` where the cat is in Home B returns 404.

### 5. The app always works within one current Home

The mobile app keeps a **current Home**, chosen from `GET /api/v1/homes` and switchable by the user. It builds every Home request's path from that Home's ID. It never sends a user ID: the user comes from its signed-in session.

## Rationale

- **One schema with a `home_id` column** suits a small app with many small tenants. It keeps one set of migrations and allows simple queries for features that cross Homes, such as "my sitting jobs this week". Schema-per-tenant and database-per-tenant add operational work with no benefit at this scale.
- **Roles on the membership, not on the user**, follow directly from the product rule that one person can be a member here and a sitter there.
- **The Home in the path, the user from authentication** gives every request both identifiers without trusting the client for either: the path says which Home the request is *about*, and the membership check decides whether this user may touch it.
- **404 for Homes without access** stops users from probing which Home IDs exist.

## Consequences

- Every new table for Home data needs `home_id`, an index starting with `home_id`, and composite foreign keys to other Home tables.
- Every new endpoint on Home data goes under `/api/v1/homes/{homeId}/` and goes through the same access check. Integration tests for such endpoints should include a request from a user of a different Home and expect 404.
- Home IDs appear in URLs and logs, so they must not be guessable. Use random UUIDs, not sequential numbers.
- Authentication has to be in place before the first Home endpoint. It's the next architectural decision.
- Sitter access ends by itself when `valid_until` passes; no cleanup job is needed for access control.

## Alternatives considered

### User ID in a header or body field

This is simple, but lets any client claim to be any user. Rejected: the user must come from verified authentication.

### Home ID in a header (`X-Home-Id`) instead of the path

This keeps URLs shorter, but hides the tenant from the URL, the OpenAPI contract and caches, and makes it easier to forget on a new endpoint. Rejected in favour of the path.

### Home ID inside the access token

This means a new token, or a token change, every time the user switches Home, and stale tokens can point at a Home the user just left. Rejected: the token proves who the user is, and the per-request membership check decides access.

### User types (`MemberUser`, `SitterUser`)

This can't represent one person who is a member of one Home and a sitter for another. Rejected in favour of per-Home roles.

### PostgreSQL row-level security

This would be a second safety net under the application's `home_id` filtering. Deferred: worth reconsidering once there are several Home tables and real users, as defence in depth.

## Revisit this decision if

- Homes need to share data with each other, for example a cat moving between Homes.
- More roles are needed (read-only viewer, vet) or a user needs more than one role in the same Home.
- The scale or isolation requirements change enough to need separate schemas or databases per tenant.
