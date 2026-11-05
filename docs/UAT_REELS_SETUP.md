# UAT reels setup (step-by-step)

Goal: **no laptop-local Postgres/Kafka for reels** — UAT uses **AWS** (RDS, MSK, ElastiCache, S3, CloudFront, ECS). **Production** reuses the same layout with `environments/prod.env.example` → `.env.prod` and profile `prod`.

## 0. One-time repo setup

```bash
cd indieestatebackend
cp environments/uat.env.example .env.uat
# Edit .env.uat: RDS URL, MSK brokers, Redis host, JWT, bucket, CDN, API_BASE_URL
chmod +x scripts/*.sh
```

Never commit `.env.uat` or `.env.prod`.

| Variable group | UAT example | Prod (later) |
|----------------|-------------|--------------|
| `SPRING_PROFILES_ACTIVE` | `postgres,api,uat` | `postgres,api,prod` |
| `REELS_S3_BUCKET` | `indiestate-reels-uat` | `indiestate-reels-prod` |
| `API_BASE_URL` | `https://uat-api.yourdomain.com` | `https://api.yourdomain.com` |

---

## 1. AWS — S3 + CloudFront (reels bytes)

1. Create **private** bucket `indiestate-reels-uat` in **ap-south-1** (block public access).
2. **CORS** — allow `GET`, `HEAD`, `PUT` (see root `README.md` JSON).
3. Create **CloudFront** distribution with **OAC** to the bucket; set `REELS_CDN_BASE_URL=https://d….cloudfront.net` in `.env.uat`.
4. **IAM** — policy with `s3:PutObject`, `GetObject`, `HeadObject`, `ListBucket`, `DeleteObject` on the bucket.
   - **ECS tasks**: attach as **task role** (leave `AWS_ACCESS_KEY_ID` empty on tasks).
   - **CI / verify scripts**: use a deploy user key in `.env.uat` locally only.

Verify:

```bash
scripts/verify-aws-credentials.sh uat
```

---

## 2. AWS — RDS PostgreSQL

1. Create **PostgreSQL 16** (e.g. `db.t4g.medium` for UAT).
2. Database name: **`indieestate`**, user/password in Secrets Manager or `.env.uat`.
3. Security group: allow **5432** from ECS tasks (and bastion if you run migrations from laptop).

Set in `.env.uat`:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<rds-endpoint>:5432/indieestate
POSTGRES_USER=...
POSTGRES_PASSWORD=...
```

On **first deploy**, Flyway runs automatically (`postgres,api,uat` → `application-uat.yml`).

---

## 3. AWS — MSK (Kafka)

1. Create **MSK** cluster (Kafka 3.x, same VPC as ECS).
2. Topics (auto-create may be on): **`video.uploaded`**, **`video.processed`**.
3. Set `KAFKA_BOOTSTRAP_SERVERS=<broker1:9092,broker2:9092>` in `.env.uat`.

Security group: ECS API + worker → MSK **9092**.

---

## 4. AWS — ElastiCache Redis

1. Redis **7** cluster (single node OK for UAT).
2. Set `REDIS_HOST`, `REDIS_PORT=6379`, `REELS_REDIS_ENABLED=true`.

---

## 5. AWS — ECS (API + media worker)

Build and push image:

```bash
docker build -t indieestate-backend .
# tag + push to ECR, then deploy two services from same image
```

### Service A — API

| Setting | Value |
|---------|--------|
| Profile / env | `SPRING_PROFILES_ACTIVE=postgres,api,uat` |
| Load balancer | **ALB** → port 8080 |
| `REELS_RUN_MEDIA_WORKER` | `false` |
| `REELS_KAFKA_ENABLED` | `true` |
| `REELS_REDIS_ENABLED` | `true` |
| Secrets | `JWT_SECRET`, DB password from Secrets Manager |

### Service B — media worker

| Setting | Value |
|---------|--------|
| Profile | `postgres,worker,uat` |
| Load balancer | **none** |
| `REELS_RUN_MEDIA_WORKER` | `true` |
| Same | DB, Kafka, S3 bucket, CDN URL, task role for S3 |

Autoscale worker on **Kafka consumer lag** (`indieestate-media-workers`).

Terraform sketch: `infra/terraform/README.md` (implement modules or mirror in console).

---

## 6. Run from laptop against UAT (optional)

If RDS/MSK are only reachable via VPN/bastion:

```bash
source scripts/load-env.sh uat
scripts/run-api.sh uat          # terminal 1
scripts/run-worker.sh uat       # terminal 2
```

Use the same `.env.uat` values ECS uses.

---

## 7. Android UAT

- Base URL = `API_BASE_URL` from `.env.uat` (ALB HTTPS).
- Reels: **`GET /api/reels/upload-config`** → `PRESIGNED_S3` only.
- Playback: HLS URLs on **CloudFront**, not through API.

---

## 8. Verify reels flow

With API + worker running and reachable at `API_BASE_URL`:

```bash
scripts/verify-reels-flow.sh uat
```

Script checks:

1. `/api/categories` (marketplace)
2. `upload-config` → `PRESIGNED_S3`
3. Register → presigned upload → S3 PUT (2s sample video) → `complete`
4. Polls feed until reel **`READY`** (needs Kafka + worker)

Requires: `curl`, `jq`, `ffmpeg` (to generate sample if missing).

---

## 9. Promote UAT → prod

1. Copy `environments/prod.env.example` → `.env.prod`.
2. New bucket + CloudFront (**prod**), RDS, MSK, Redis, ECS cluster/services.
3. Change only **values**; same keys as UAT.
4. Profiles: `postgres,api,prod` and `postgres,worker,prod`.
5. `AUTH_OTP_DEV_MODE=false`, real SMS, strong secrets, Swagger off (`application-prod.yml`).

```bash
scripts/verify-aws-credentials.sh prod
scripts/verify-reels-flow.sh prod
```

---

## Troubleshooting

| Symptom | Check |
|---------|--------|
| S3 PUT 403 | IAM, bucket name, SSE headers from presign |
| Stuck `PROCESSING` | Worker running? MSK reachable? Consumer group lag |
| Flyway fail on start | RDS empty vs migration conflict; connect with `validate` profile |
| `PRESIGNED_S3` missing | `REELS_SKIP_S3`, empty bucket, fake storage |

See also: `docs/REELS_ARCHITECTURE.md`, `docs/REELS_DEBUG.md`.
