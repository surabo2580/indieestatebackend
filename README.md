# Indieestate Backend

Kotlin + Spring Boot API for classifieds and home services. Clients fetch categories or services, load a dynamic form schema, then submit an ad.

Setup follows the same local run pattern as RewardBackend: Homebrew PostgreSQL, `surajdas` as the DB user, and `./start-backend.sh`.

## Stack

- Kotlin 2.2, Java 17
- Spring Boot 4.0
- PostgreSQL (default) or H2 (`h2` profile)
- Flyway for Postgres schema/seed
- JWT (HS256) + BCrypt
- OpenAPI / Swagger UI

## Run locally

Uses your existing Homebrew Postgres user (`surajdas`), same as RewardBackend. Creates the `indieestate` database if it is missing.

```bash
./start-backend.sh
```

Or:

```bash
./gradlew bootRun
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/api/health

In-memory H2 (no Postgres):

```bash
SPRING_PROFILES_ACTIVE=h2 ./gradlew bootRun
```

## Environment

Same variable names as RewardBackend:

| Variable | Default |
|---|---|
| `PORT` | `8080` |
| `POSTGRES_DB` | `indieestate` |
| `POSTGRES_USER` | `surajdas` |
| `POSTGRES_PASSWORD` | `surajdas` |
| `SPRING_DATASOURCE_URL` / `DB_URL` | `jdbc:postgresql://localhost:5432/indieestate` |
| `JWT_SECRET` | local default — change in any shared environment |
| `JWT_ISSUER` | `indieestate-backend` |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | `7200` |

Copy `.env.example` if you want a local env file.

## API flow

1. `GET /api/categories` — jobs, cars, bikes, properties
2. `GET /api/services` — home cleaning, salon, water services, plumbing
3. `GET /api/forms?categoryId={id}` or `?serviceId={id}` or `?code=CAR_SELL`
4. Render the schema on the client, collect values
5. `POST /api/auth/register` or `POST /api/auth/login`
6. `POST /api/ads` with the JWT and filled `data`

Protected routes: `GET /api/auth/me`, `POST /api/ads`, `GET /api/ads/me`.

## Tests

```bash
./gradlew test
```

Tests use the `h2` profile, same idea as RewardBackend.
