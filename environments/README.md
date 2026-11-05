# Environment configuration (UAT vs prod)

Use **the same variable names** in every environment; only values change. This keeps ECS task definitions and Android build flavors aligned.

| File | Purpose |
|------|---------|
| `uat.env.example` | Template for **UAT** — copy to **`.env.uat`** at repo root |
| `prod.env.example` | Template for **production** — copy to **`.env.prod`** |
| `../.env.example` | Optional **local** laptop dev only |

## Spring profiles

| Environment | API process | Worker process |
|-------------|-------------|------------------|
| UAT | `postgres,api,uat` | `postgres,worker,uat` |
| Prod | `postgres,api,prod` | `postgres,worker,prod` |

UAT and prod both use **Flyway** + Hibernate **`validate`** (no `ddl-auto: update`).

## Commands

```bash
# Load UAT env into current shell
source scripts/load-env.sh uat

# Run API against UAT RDS/MSK (from laptop or bastion)
scripts/run-api.sh uat

# Run media worker against UAT
scripts/run-worker.sh uat

# Verify AWS + optional live API reels smoke test
scripts/verify-aws-credentials.sh uat
scripts/verify-reels-flow.sh uat
```

Full UAT reels setup: **[docs/UAT_REELS_SETUP.md](../docs/UAT_REELS_SETUP.md)**.
