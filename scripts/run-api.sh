#!/usr/bin/env bash
set -euo pipefail
ENV_NAME="${1:-uat}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# shellcheck disable=SC1091
source "$ROOT/scripts/load-env.sh" "$ENV_NAME"

export REELS_RUN_MEDIA_WORKER="${REELS_RUN_MEDIA_WORKER:-false}"

exec ./gradlew bootRun "${@:2}"
