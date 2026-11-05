# Indieestate reels — AWS production (Terraform sketch)

This layout targets **ECS Fargate** (API + media worker), **RDS PostgreSQL**, **ElastiCache Redis**, **MSK Kafka**, and **IAM task roles** for S3 (no long-lived keys on tasks).

For the full **Instagram-aligned** step map (ingress → S3 → Kafka → media worker → CloudFront → feed), see [`docs/ARCHITECTURE.md`](../../docs/ARCHITECTURE.md).

| Reference step | AWS / app |
|----------------|-----------|
| 1 Secure upload | ALB → ECS **Ingestion API** (`ReelController`) |
| 2 Raw to object storage | Presigned PUT → **S3** `raw/` |
| 3 Event | **`POST /complete`** → **MSK** `video.uploaded` |
| 4–5 Transcode | ECS **MediaProcessingService** (ffmpeg) |
| 6–7 CDN playback | **S3** `media/` + **CloudFront** |
| 8 Feed + cache | ECS **FeedService** + **ElastiCache** |

## Prerequisites

- Terraform >= 1.5
- AWS CLI configured (`ap-south-1` or your region)
- Existing S3 bucket `indiestate-reels-uat` and CloudFront distribution

## Modules

| Path | Purpose |
|------|---------|
| `modules/network` | VPC, public/private subnets, NAT |
| `modules/rds` | PostgreSQL 16 |
| `modules/elasticache` | Redis 7 |
| `modules/msk` | Kafka 3.x cluster |
| `modules/ecs` | Cluster, ECR, API + worker services, ALB |
| `modules/iam` | Task roles: S3 read/write on reels bucket |

## Deploy order

1. `terraform init`
2. `terraform apply -target=module.network`
3. `terraform apply -target=module.rds -target=module.elasticache -target=module.msk`
4. Build and push image: `docker build -t indieestate-backend .` → ECR
5. `terraform apply` (ECS services)

## ECS task environment

**API service** (`SPRING_PROFILES_ACTIVE=postgres,api,prod`):

- `REELS_KAFKA_ENABLED=true`
- `REELS_RUN_MEDIA_WORKER=false`
- `REELS_REDIS_ENABLED=true`
- `KAFKA_BOOTSTRAP_SERVERS=<MSK bootstrap brokers>`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://<RDS endpoint>/indieestate`
- `REDIS_HOST=<ElastiCache primary>`
- `REELS_S3_BUCKET`, `REELS_CDN_BASE_URL`, `AWS_REGION` (credentials via **task role**)

**Media worker** (`SPRING_PROFILES_ACTIVE=postgres,worker,prod`):

- `REELS_KAFKA_ENABLED=true`
- `REELS_RUN_MEDIA_WORKER=true`
- Same DB/Kafka/S3 env as API (no ALB)

## Secrets

Store in **Secrets Manager** and inject as task secrets:

- `JWT_SECRET`
- `POSTGRES_PASSWORD`
- Optional `REELS_ADMIN_RECONCILE_TOKEN`

## Autoscaling

- API: target tracking on ALB request count / CPU
- Worker: custom metric MSK `OffsetLag` for consumer group `indieestate-media-workers`

## Local parity

```bash
docker compose -f docker-compose.yml -f docker-compose.app.yml up --build
```

Host dev (API on laptop, Kafka in Docker):

```bash
docker compose up postgres redis kafka -d
./start-backend.sh          # REELS_KAFKA_ENABLED=true in .env
./scripts/start-media-worker.sh
```
