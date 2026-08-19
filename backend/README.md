# TradeBall Backend

Production-oriented Spring Boot modular monolith powering TradeBall fantasy basketball trade analysis.

## Stack

- Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Spring Security (JWT)
- PostgreSQL + Flyway
- Redis caching (optional; falls back to Caffeine)
- OpenAPI/Swagger
- JUnit 5, Mockito, Testcontainers (optional)
- Docker Compose

## Quick start (Docker)

```bash
cd backend
docker compose up --build
```

API: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html  
Health: http://localhost:8080/actuator/health

`docker compose` is a local development environment: it activates `docker,dev`, seeds a curated development snapshot, and persists PostgreSQL data in the `tradeball_pg` volume. It must not be used as a production deployment recipe.

Seeded users (the explicit `dev` profile only):

| Email | Password | Role |
|-------|----------|------|
| admin@tradeball.local | Admin123! | ADMIN |
| demo@tradeball.local | Demo1234! | USER |

## Local development (without Docker app container)

1. Start Postgres + Redis (or use compose infra only):

```bash
docker compose up -d postgres redis
```

2. Configure environment variables. `.env.example` is a reference file; Spring Boot does not load `.env` files automatically. For PowerShell, set them for the current shell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:JWT_SECRET = "replace-with-a-local-development-secret-at-least-32-bytes"
```

3. Run:

```bash
mvn spring-boot:run
```

The `dev` profile is intentional: it enables the curated development NBA snapshot and seeded accounts. Do not enable it in production. The default profile requires `JWT_SECRET` to be supplied and uses the HTTP NBA provider.

## Configuration

All production-sensitive values are environment variables. See `.env.example` for the complete local reference.

| Variable | Purpose |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL JDBC connection. `DATABASE_URL` must be `jdbc:postgresql://...`, not `postgres://` |
| `DATABASE_MAX_POOL_SIZE` | Hikari maximum pool size (default `10`) |
| `FLYWAY_ENABLED` | Flyway on startup (default `true`; keep enabled in production) |
| `REDIS_HOST`, `REDIS_PORT` | Redis cache connection |
| `JWT_SECRET` | Required outside `dev`/`test`; random secret of at least 32 bytes. Documented development/default values are rejected unless `dev` or `test` is active |
| `JWT_EXPIRATION_MS` | Access-token lifetime (default: one day) |
| `NBA_API_BASE_URL`, `NBA_API_SEASON` | NBA provider configuration |
| `NBA_API_USE_DEV_FALLBACK` | Development-only curated snapshot; default `false` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated browser/mobile client origins |

## Migrations, API docs, and verification

Flyway applies versioned migrations from `src/main/resources/db/migration` on startup; Hibernate validates rather than creates the schema. Swagger UI is available at `/swagger-ui.html` and its JSON document at `/api-docs`. Actuator exposes only `/actuator/health` and `/actuator/info`.

```bash
mvn clean test
mvn verify
mvn -DskipTests package
docker compose config
docker compose build
docker compose up -d
docker compose ps
docker compose logs --tail=200
```

For production, provide managed PostgreSQL and Redis, set a unique `JWT_SECRET`, set an explicit CORS allow-list, keep `NBA_API_USE_DEV_FALLBACK=false`, and run the image with the `docker` profile only. The frontend consumes the API through `www/js/tradeball-api.js`; it sends JWTs as Bearer tokens and never calculates authoritative trade scores itself.

### Production PostgreSQL (Supabase)

Local Docker Compose keeps using the `postgres` service. Production overrides the same variables on the host; no secrets belong in Git.

Set these on the cloud app (not in compose):

| Variable | What to enter |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://HOST:5432/postgres?sslmode=require` |
| `DATABASE_USERNAME` | Database user from the Supabase dashboard (direct: `postgres`; session pooler: `postgres.PROJECT_REF`) |
| `DATABASE_PASSWORD` | Database password from the dashboard (never commit) |
| `DATABASE_MAX_POOL_SIZE` | Optional; `10` is the default. Lower it if the instance connection limit is tight |
| `FLYWAY_ENABLED` | Leave unset or `true`. Flyway applies `db/migration` on startup |

Convert the dashboard URI. Example shape only:

```text
postgres://USER:PASSWORD@HOST:5432/postgres
```

becomes:

```text
DATABASE_URL=jdbc:postgresql://HOST:5432/postgres?sslmode=require
DATABASE_USERNAME=USER
DATABASE_PASSWORD=PASSWORD
```

Use the **direct** connection (`db.<project-ref>.supabase.co:5432`) or the **session-mode** pooler (port **5432**). Do not point this app at the **transaction** pooler (port **6543**) while Flyway shares the datasource. Include `sslmode=require`. Do not embed the password in `DATABASE_URL` if the host can set `DATABASE_PASSWORD` separately.

## Tests

```bash
mvn clean test
```

Deterministic unit tests cover fantasy z-scores, trade scoring, buy-low/sell-high, and roster authorization. API tests cover auth, players, rosters, and trades against H2.

## Key endpoints

| Method | Path | Auth |
|--------|------|------|
| POST | /api/v1/auth/register | public |
| POST | /api/v1/auth/login | public |
| GET | /api/v1/auth/me | JWT |
| GET | /api/v1/players | public |
| GET | /api/v1/players/search?q= | public |
| GET | /api/v1/players/{id} | public |
| GET | /api/v1/players/{id}/stats | public |
| GET | /api/v1/players/{id}/fantasy-value | public |
| CRUD | /api/v1/rosters... | JWT (owner) |
| POST | /api/v1/trades/evaluate | JWT |
| GET | /api/v1/trades | JWT |
| POST | /api/v1/admin/sync/players | ADMIN |
| POST | /api/v1/admin/sync/stats | ADMIN |
| GET | /api/v1/admin/sync/status | ADMIN |

## NBA integration

External calls are isolated behind `NbaStatsClient`.

- `tradeball.nba.use-dev-fallback=true`: **DEVELOPMENT-ONLY** curated snapshot matching the frontend fallback data. It is set only by `application-dev.yml` or the local compose environment. Live NBA stats are still tried first.
- `false` (the production default): live HTTP client against `tradeball.nba.base-url`.

Trade evaluation sums 9-cat z-score production on each side. Averaging made 1-for-3 packages look even; that is no longer used. Dynatyze's proprietary Master Value / crowd model is not available as a public API, so TradeBall uses the standard 9-cat z-score total (Hashtag-style) against live per-game NBA stats.

Never hard-code API keys. Configure via environment variables.

## Frontend

Point the web client at `http://localhost:8080/api/v1` using `www/js/tradeball-api.js`.
