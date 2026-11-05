#!/usr/bin/env bash
set -euo pipefail
ENV_NAME="${1:-uat}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# shellcheck disable=SC1091
source "$ROOT/scripts/load-env.sh" "$ENV_NAME"

case "$ENV_NAME" in
	uat) WORKER_PROFILES="postgres,worker,uat" ;;
	prod) WORKER_PROFILES="postgres,worker,prod" ;;
	*) WORKER_PROFILES="postgres,worker" ;;
esac

export SPRING_PROFILES_ACTIVE="$WORKER_PROFILES"
export REELS_KAFKA_ENABLED="${REELS_KAFKA_ENABLED:-true}"
export REELS_RUN_MEDIA_WORKER=true

exec ./gradlew bootRun "${@:2}"
