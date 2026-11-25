# Infra: home-network deployment

One Docker Compose stack on a machine on the home network:

| Service | Role |
|---|---|
| `proxy` | Caddy. Terminates HTTPS, serves the Expo web build from `mobile/dist`, routes `/api/*` to the API |
| `api` | Spring Boot (`api/`), reachable only inside the stack on port 8080 |
| `db` | PostgreSQL 18, data kept in the `db-data` volume; published on `127.0.0.1` only |

## Deploy

From the repository root:

```bash
cp infra/.env.example infra/.env   # then fill in SITE_ADDRESS, DATABASE_PASSWORD and JWT_PRIVATE_KEY
(cd mobile && npm ci && EXPO_PUBLIC_API_URL= npm run build:web)
docker compose -f infra/compose.yaml --env-file infra/.env up -d --build
```

`JWT_PRIVATE_KEY` is the key that signs sign-in tokens ([ADR-004](../docs/adr/ADR-004-authentication.md)). Generate it once and keep it; replacing it signs everyone out:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -outform DER | base64 | tr -d '\n'
```

`EXPO_PUBLIC_API_URL=` (empty) makes the web app call `/api` on the address it was loaded from. It overrides any local-dev value in `mobile/.env`, such as `http://localhost:8080`, which would otherwise be baked into the bundle and break the app on the phone.

Open `https://<SITE_ADDRESS>` on the phone.

## Updating

| Changed | Do |
|---|---|
| Web app (`mobile/`) | Rebuild with the `build:web` command above; Caddy serves the new files right away |
| API (`api/`) | `docker compose up -d --build api` |
| `Caddyfile` | `docker compose restart proxy`; Caddy only sees the edited file after a restart |

## Trusting the certificate on the phone

`tls internal` makes Caddy issue certificates from its own local CA. Copy the root certificate out and install it on the phone as a trusted certificate:

```bash
docker compose -f infra/compose.yaml cp proxy:/data/caddy/pki/authorities/local/root.crt ./caddy-root.crt
```

Never commit `infra/.env` or the exported certificate.
