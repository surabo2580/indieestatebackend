# Reels architecture (scalable / Instagram-style)

Platform-wide mapping (creator steps 1–7, viewer step 8, scaling phases): **[ARCHITECTURE.md](ARCHITECTURE.md)**.

This is the **production** path. Do not route phone video through the Spring Boot JVM in production — that does not scale.

## Flow

```mermaid
sequenceDiagram
    participant App
    participant API as Spring API
    participant S3
    participant Worker as Media worker
    participant CDN as CloudFront
    participant PG as PostgreSQL

    App->>API: POST /api/reels/uploads (JWT)
    API->>PG: videos row UPLOADING
    API-->>App: presigned PUT URL + videoId + objectKey
    App->>S3: PUT raw/{videoId}/original.mp4 (direct, no API bytes)
    App->>API: POST /api/reels/complete (JWT)
    API->>PG: reels row PROCESSING
    API->>Worker: video.uploaded (Kafka or in-process)
    Worker->>S3: GET raw, ffmpeg HLS ladder
    Worker->>S3: PUT media/{videoId}/hls/...
    Worker->>PG: video READY, reel READY, videoUrl = CDN master.m3u8
    App->>API: GET /api/reels
    API-->>App: HLS URL on CloudFront
    App->>CDN: stream adaptive bitrate
```

## Why presigned S3?

| Concern | Presigned upload | Multipart via API |
|--------|------------------|-------------------|
| API CPU / memory | Minimal | High (every byte through JVM) |
| Horizontal scale | Add API nodes; S3 absorbs upload | API becomes bottleneck |
| CDN playback | HLS on CloudFront | Would need separate pipeline |
| Same as Reels/TikTok/IG | Yes | No (dev shortcut only) |

## Configuration checklist (fix 403)

Your backend log `Cannot access reels bucket … Forbidden (403)` means **the IAM user in `.env` cannot access the bucket**. The phone will get **403 on PUT** for the same reason. This is **not** an Android CORS issue.

1. **Bucket name** — must match exactly (e.g. `indiestate-reels-uat` in `ap-south-1`). Typos in `.env` cause HeadBucket/PutObject failures.

2. **IAM policy** (attach to the user whose keys are in `AWS_ACCESS_KEY_ID`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "IndieestateReelsBucket",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:HeadObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::indiestate-reels-uat",
        "arn:aws:s3:::indiestate-reels-uat/*"
      ]
    }
  ]
}
```

3. **Bucket policy** — must allow that IAM principal (or use IAM-only access on a private bucket with Block Public Access on).

4. **Presigned PUT** — IAM must include `s3:PutObject` on `raw/*`. No ACL required if using bucket-owner-enforced object ownership.

5. **CORS** — required for **web** clients; configure PUT/GET on the bucket (see root README). Native Android/iOS use HTTP PUT to the presigned URL; CORS does not apply there.

6. **CloudFront** — `REELS_CDN_BASE_URL` must point at the distribution that reads `media/` keys via OAC/OAI.

7. **Verify** — restart backend; log should say `Reels S3 bucket '…' is reachable`. Then upload from the app.

## Dev-only escape hatch

`REELS_ALLOW_DIRECT_UPLOAD=true` + `REELS_SKIP_S3=true` exposes `DIRECT_MULTIPART` in `GET /api/reels/upload-config`. The app uploads via `POST /api/reels/upload` **only in that mode**. Never enable in production.

## Production scale-out (implemented)

| Process | Profile | Kafka | ffmpeg |
|---------|---------|-------|--------|
| API | `api` | producer only (`REELS_RUN_MEDIA_WORKER=false`) | no |
| Media worker | `worker` | consumer (`REELS_RUN_MEDIA_WORKER=true`) | yes |

- Topics: `video.uploaded`, `video.processed` under `app.reels.kafka` in `application.yml`.
- Feed: `GET /api/reels?limit=20&cursor=…` returns `{ items, nextCursor }` (Postgres is catalog; S3 is not listed).
- Stale uploads: API job marks `UPLOADING` videos older than `REELS_UPLOAD_STALE_HOURS` as `EXPIRED` and deletes `raw/` when possible.
- S3 lifecycle: expire incomplete `raw/` after 7–14 days in the AWS console (recommended).
- Reconcile: `GET /api/admin/reels/storage-reconcile` with header `X-Reels-Admin-Token` (set `REELS_ADMIN_RECONCILE_TOKEN`).
- Metrics: `reels.transcode.duration`, `reels.transcode.processed`, `reels.transcode.failed` (Actuator/Micrometer).
- AWS deploy sketch: [infra/terraform/README.md](../infra/terraform/README.md).

### Local full stack

```bash
docker compose -f docker-compose.yml -f docker-compose.app.yml up --build
```

### Local split (API on host)

```bash
docker compose up postgres redis kafka -d
# .env: REELS_KAFKA_ENABLED=true, REELS_RUN_MEDIA_WORKER=false
./start-backend.sh
./scripts/start-media-worker.sh
```

### Transcode providers

- `REELS_TRANSCODE_PROVIDER=ffmpeg` (default) — worker runs ffmpeg.
- `mediaconvert` — stub for future AWS Elemental MediaConvert.

## Dev-only escape hatch (reminder)

`REELS_ALLOW_DIRECT_UPLOAD=true` + `REELS_SKIP_S3=true` exposes `DIRECT_MULTIPART`. Never enable in production.
