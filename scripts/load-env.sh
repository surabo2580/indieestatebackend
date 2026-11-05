#!/usr/bin/env bash
# Usage: source scripts/load-env.sh uat
# Loads repo-root .env.uat (or .env.prod, .env.local).
set -euo pipefail

ENV_NAME="${1:-}"
if [[ -z "$ENV_NAME" ]]; then
	echo "Usage: source scripts/load-env.sh <uat|prod|local>" >&2
	return 1 2>/dev/null || exit 1
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT/.env.$ENV_NAME"
if [[ "$ENV_NAME" == "local" && ! -f "$ENV_FILE" && -f "$ROOT/.env" ]]; then
	ENV_FILE="$ROOT/.env"
fi

if [[ ! -f "$ENV_FILE" ]]; then
	echo "Missing $ENV_FILE" >&2
	echo "Copy: cp $ROOT/environments/${ENV_NAME}.env.example $ENV_FILE" >&2
	return 1 2>/dev/null || exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

export APP_ENV="${APP_ENV:-$ENV_NAME}"
case "$ENV_NAME" in
	uat)
		export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-postgres,api,uat}"
		;;
	prod)
		export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-postgres,api,prod}"
		;;
	local)
		export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-postgres,api}"
		;;
esac

echo "Loaded $ENV_FILE (APP_ENV=$APP_ENV, SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE)"
