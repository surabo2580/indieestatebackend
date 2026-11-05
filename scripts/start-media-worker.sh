#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ -f .env ]]; then
	set -a
	# shellcheck disable=SC1091
	source .env
	set +a
fi
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-postgres},worker"
export REELS_KAFKA_ENABLED="${REELS_KAFKA_ENABLED:-true}"
export REELS_RUN_MEDIA_WORKER=true
./gradlew bootRun "$@"
