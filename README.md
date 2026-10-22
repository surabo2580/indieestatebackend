# Indieestate Backend

Kotlin + Spring Boot API for classifieds and home services. Clients fetch categories or services, load a dynamic form schema, then submit an ad.

Setup follows the same local run pattern as RewardBackend: Homebrew PostgreSQL, `surajdas` as the DB user, and `./start-backend.sh`.

## Stack

- Kotlin 2.2, Java 17
- Spring Boot 4.0
- PostgreSQL (default) or H2 (`h2` profile)
- Redis (optional feed cache)
- Kafka (optional upload → media worker)
- Flyway SQL under `db/migration` (optional; app uses Hibernate `ddl-auto: update` + startup seeders)
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

Optional Kafka + Redis (V2 pipeline):

```bash
docker compose up -d kafka redis
```

Then set `REELS_KAFKA_ENABLED=true` and `REELS_REDIS_ENABLED=true` in `.env`. Without those flags the API still works: uploads go to S3 (or fake storage in tests) and a local media worker transcodes immediately.

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
| `REELS_S3_BUCKET` | empty = fake in-memory storage |
| `REELS_CDN_BASE_URL` | CloudFront origin URL |
| `AWS_REGION` | `ap-south-1` |
| `REELS_FFMPEG_PATH` | `ffmpeg` |
| `REELS_KAFKA_ENABLED` | `false` |
| `REELS_REDIS_ENABLED` | `false` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |

Copy `.env.example` if you want a local env file.

## Data persistence (important)

**Restarting `./start-backend.sh` does not delete your data.** Ads, users, chats, and custom forms live in PostgreSQL (`POSTGRES_DB`, default `indieestate`) and stay there across restarts.

On startup:

- **Catalog** (categories/services) is inserted only when the catalog tables are empty.
- **Default form schemas** (`DefaultFormsInitializer`) are inserted only when a given `code` is missing — existing rows are never updated or deleted.

If Sell shows “No form available”, the DB is missing those form rows (often on a fresh DB). Restart the backend once after pulling latest code, or run `scripts/seed-service-forms.sql` for service forms. User-created ads must be posted again only if the database itself was dropped or recreated (`dropdb indieestate` / new machine).

**Reels (Play):** Scalable path is **presigned S3 upload → complete → transcode → CloudFront HLS**. See **`docs/REELS_ARCHITECTURE.md`**. Clients call `GET /api/reels/upload-config` (`PRESIGNED_S3` in production). Metadata is in Postgres; binaries are not stored in the JVM long-term.

**Dev-only:** `REELS_ALLOW_DIRECT_UPLOAD=true` and `REELS_SKIP_S3=true` switches config to `DIRECT_MULTIPART` (legacy `POST /api/reels/upload`). Do not use in production.

**403 on upload:** fix AWS IAM / bucket name — not fixed by routing video through the API. Restart backend until logs show the bucket is reachable.

## Reels: S3 + CloudFront (Play)

Postgres stores video metadata only (`videos`, `video_renditions`, `reels`). The binary lives in S3. Phones upload with a presigned PUT, Kafka (or a local worker) transcodes to multi-bitrate HLS, and Play streams the master playlist from CloudFront. Media3 / AVPlayer pick 1080 / 720 / 480 / 360 from that playlist.

S3 keys:

```
raw/{videoId}/original.mp4
media/{videoId}/hls/master.m3u8
media/{videoId}/hls/{1080p,720p,480p,360p}/playlist.m3u8
media/{videoId}/thumbnail/poster.jpg
```

Video status: `UPLOADING` → `UPLOADED` → `PROCESSING` → `READY` or `FAILED`. `GET /api/reels` returns `READY` reels only. Do not put AWS keys in git.

1. Create a **private** bucket (example: `indiestate-reels-uat` in `ap-south-1`). Block all public access.
2. Bucket CORS (PUT from the app, GET from the player):

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "HEAD", "PUT"],
    "AllowedOrigins": ["*"],
    "ExposeHeaders": ["ETag", "Accept-Ranges", "Content-Length"],
    "MaxAgeSeconds": 3000
  }
]
```

3. CloudFront distribution with Origin Access Control to that bucket. HTTPS only. Copy the domain (`https://dxxxxx.cloudfront.net`).
4. IAM user limited to `s3:PutObject`, `s3:GetObject`, `s3:HeadObject`, `s3:ListBucket`, `s3:DeleteObject` on that bucket. Create an access key.
5. `brew install ffmpeg`
6. Copy `.env.example` to `.env` and set `AWS_*`, `REELS_S3_BUCKET`, `REELS_CDN_BASE_URL`.

Without `REELS_S3_BUCKET`, the backend uses in-memory fake storage (H2 tests). Playback on a phone needs real S3 + CloudFront so video does not go through `adb reverse`.

## API flow

1. `GET /api/categories` — jobs, cars, bikes, properties
2. `GET /api/services` — home cleaning, salon, water services, plumbing
3. `GET /api/forms?categoryId={id}` or `?serviceId={id}` or `?code=CAR_SELL`
4. Render the schema on the client, collect values
5. `POST /api/auth/register` or `POST /api/auth/login`
6. `POST /api/ads` with the JWT and filled `data`
7. `GET /api/reels/upload-config` — `PRESIGNED_S3` (production) or `DIRECT_MULTIPART` (dev only)
8. `GET /api/reels` — Play feed (`READY` + in-flight `PROCESSING`; Redis cache when enabled)
9. `POST /api/reels/uploads` — creates a `videos` row (`UPLOADING`) and a presigned S3 PUT to `raw/{videoId}/original.*` (JWT)
9. `POST /api/reels/complete` — marks the video `UPLOADED`, publishes `video.uploaded`, worker writes ABR HLS (JWT)
10. `POST /api/reels` — publish a reel from an existing URL (JWT, tests / legacy)
11. `POST /api/reels/upload` — legacy gallery multipart through the API (JWT)
12. `GET /api/reels/media/{filename}` — stream a legacy locally stored video

Protected routes: `GET /api/auth/me`, `POST /api/ads`, `GET /api/ads/me`, `POST /api/reels`, `POST /api/reels/uploads`, `POST /api/reels/complete`.

## Tests

```bash
./gradlew test
```

Tests use the `h2` profile, same idea as RewardBackend.
