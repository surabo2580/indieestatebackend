#!/usr/bin/env bash
# End-to-end reels smoke test against a running API (UAT/prod).
# Usage: scripts/verify-reels-flow.sh uat
# Requires: curl, jq, aws CLI; optional ffmpeg to generate a 2s sample MP4.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ENV_NAME="${1:-uat}"
ENV_FILE="$ROOT/.env.$ENV_NAME"
if [[ ! -f "$ENV_FILE" ]]; then
	echo "Missing $ENV_FILE"
	exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

BASE_URL="${API_BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
SAMPLE="${REELS_VERIFY_SAMPLE_VIDEO:-$ROOT/scripts/fixtures/uat-smoke.mp4}"
WAIT_READY_SEC="${REELS_VERIFY_WAIT_SEC:-180}"

need() {
	command -v "$1" >/dev/null 2>&1 || {
		echo "Missing required command: $1"
		exit 1
	}
}
need curl
need jq

echo "=== Reels flow verify ($ENV_NAME) ==="
echo "API: $BASE_URL"

echo ""
echo "→ GET /api/categories (marketplace liveness)"
curl -sf "$BASE_URL/api/categories" | jq 'if type == "array" then length else . end'

echo ""
echo "→ GET /api/reels/upload-config"
config="$(curl -sf "$BASE_URL/api/reels/upload-config")"
strategy="$(echo "$config" | jq -r '.strategy')"
echo "$config" | jq .
if [[ "$strategy" != "PRESIGNED_S3" ]]; then
	echo "FAIL: expected strategy PRESIGNED_S3, got $strategy"
	exit 1
fi

EMAIL="reels-verify-$(date +%s)@uat.indieestate.test"
echo ""
echo "→ POST /api/auth/register ($EMAIL)"
reg="$(curl -sf -X POST "$BASE_URL/api/auth/register" \
	-H "Content-Type: application/json" \
	-d "{\"name\":\"Reels Verify\",\"email\":\"$EMAIL\",\"password\":\"password1\"}")"
TOKEN="$(echo "$reg" | jq -r '.accessToken')"
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
	echo "FAIL: no accessToken"
	exit 1
fi

echo ""
echo "→ POST /api/reels/uploads"
upload="$(curl -sf -X POST "$BASE_URL/api/reels/uploads" \
	-H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" \
	-d '{"fileName":"uat-smoke.mp4","contentType":"video/mp4"}')"
echo "$upload" | jq .
VIDEO_ID="$(echo "$upload" | jq -r '.videoId')"
OBJECT_KEY="$(echo "$upload" | jq -r '.objectKey')"
UPLOAD_URL="$(echo "$upload" | jq -r '.uploadUrl')"
if [[ -z "$VIDEO_ID" || "$VIDEO_ID" == "null" ]]; then
	echo "FAIL: upload session"
	exit 1
fi

if [[ ! -f "$SAMPLE" ]]; then
	if command -v ffmpeg >/dev/null 2>&1; then
		mkdir -p "$(dirname "$SAMPLE")"
		echo "Generating 2s sample: $SAMPLE"
		ffmpeg -y -loglevel error -f lavfi -i testsrc=duration=2:size=640x360:rate=30 \
			-c:v libx264 -pix_fmt yuv420p -t 2 "$SAMPLE"
	else
		echo "WARN: No $SAMPLE and no ffmpeg — skipping S3 PUT and transcode wait."
		echo "OK through presigned upload session only."
		exit 0
	fi
fi

echo ""
echo "→ PUT object to S3 (presigned URL)"
CURL_HEADERS=()
while IFS= read -r line; do
	[[ -z "$line" ]] && continue
	key="${line%%=*}"
	val="${line#*=}"
	CURL_HEADERS+=(-H "$key: $val")
done < <(echo "$upload" | jq -r '.uploadHeaders | to_entries[] | "\(.key)=\(.value)"')

HTTP_CODE="$(curl -sS -o /dev/null -w "%{http_code}" -X PUT "$UPLOAD_URL" \
	-H "Content-Type: video/mp4" \
	"${CURL_HEADERS[@]}" \
	--data-binary "@$SAMPLE")"
if [[ "$HTTP_CODE" != "200" && "$HTTP_CODE" != "204" ]]; then
	echo "FAIL: S3 PUT returned HTTP $HTTP_CODE (check IAM / bucket / SSE headers)"
	exit 1
fi
echo "S3 PUT HTTP $HTTP_CODE"

echo ""
echo "→ POST /api/reels/complete"
complete="$(curl -sf -X POST "$BASE_URL/api/reels/complete" \
	-H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" \
	-d "{
		\"title\":\"UAT smoke reel\",
		\"caption\":\"verify-reels-flow\",
		\"category\":\"Properties\",
		\"priceLabel\":\"Test\",
		\"location\":\"UAT\",
		\"objectKey\":\"$OBJECT_KEY\",
		\"videoId\":\"$VIDEO_ID\"
	}")"
REEL_ID="$(echo "$complete" | jq -r '.id')"
STATUS="$(echo "$complete" | jq -r '.status')"
echo "$complete" | jq .
if [[ "$STATUS" != "PROCESSING" && "$STATUS" != "READY" ]]; then
	echo "WARN: expected PROCESSING or READY after complete, got $STATUS"
fi

if [[ "$STATUS" == "READY" ]]; then
	echo ""
	echo "OK: reel READY immediately (worker or inline transcode)."
	exit 0
fi

echo ""
echo "→ Waiting up to ${WAIT_READY_SEC}s for READY (Kafka + media worker must be running)..."
deadline=$((SECONDS + WAIT_READY_SEC))
while (( SECONDS < deadline )); do
	page="$(curl -sf "$BASE_URL/api/reels?limit=50")"
	item_status="$(echo "$page" | jq -r --arg id "$REEL_ID" '.items[] | select(.id == $id) | .status' | head -1)"
	video_url="$(echo "$page" | jq -r --arg id "$REEL_ID" '.items[] | select(.id == $id) | .videoUrl' | head -1)"
	if [[ "$item_status" == "READY" ]]; then
		echo "OK: reel READY"
		echo "videoUrl: $video_url"
		if [[ -n "${REELS_CDN_BASE_URL:-}" && "$video_url" == *"$REELS_CDN_BASE_URL"* ]]; then
			echo "OK: videoUrl uses configured CDN base"
		fi
		exit 0
	fi
	if [[ "$item_status" == "FAILED" ]]; then
		echo "FAIL: reel transcode FAILED"
		exit 1
	fi
	sleep 5
done

echo "FAIL: timed out waiting for READY — check MSK, worker REELS_RUN_MEDIA_WORKER=true, ffmpeg logs"
exit 1
