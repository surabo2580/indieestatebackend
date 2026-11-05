#!/usr/bin/env bash
# Verify IAM + reels bucket. Usage: scripts/verify-aws-credentials.sh [uat|prod|local]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ENV_NAME="${1:-local}"
ENV_FILE="$ROOT/.env.$ENV_NAME"
if [[ "$ENV_NAME" == "local" && -f "$ROOT/.env" ]]; then
	ENV_FILE="$ROOT/.env"
fi

if [[ ! -f "$ENV_FILE" ]]; then
	echo "Missing $ENV_FILE — copy from environments/${ENV_NAME}.env.example"
	exit 1
fi

echo "Loading: $ENV_FILE"
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${AWS_REGION:=ap-south-1}"
: "${REELS_S3_BUCKET:=indiestate-reels-uat}"

echo "Environment: $ENV_NAME"
echo "Region: $AWS_REGION"
echo "Bucket: $REELS_S3_BUCKET"
echo "Access key id: ${AWS_ACCESS_KEY_ID:0:4}…${AWS_ACCESS_KEY_ID: -4} (length ${#AWS_ACCESS_KEY_ID})"
if [[ ${#AWS_ACCESS_KEY_ID} -ne 20 ]]; then
	echo "WARN: AWS access key ids are normally 20 characters."
fi
if [[ ${#AWS_SECRET_ACCESS_KEY} -lt 40 ]]; then
	echo "WARN: Secret looks too short — copy the full secret from IAM (one line, no spaces)."
fi

echo ""
echo "→ sts get-caller-identity"
if ! aws sts get-caller-identity --region "$AWS_REGION"; then
	echo ""
	echo "InvalidClientTokenId = wrong/deleted/rotated key, or secret does not match this access key id."
	exit 1
fi

echo ""
echo "→ s3 ls s3://${REELS_S3_BUCKET}/ (first 5 lines)"
if ! aws s3 ls "s3://${REELS_S3_BUCKET}/" --region "$AWS_REGION" | head -5; then
	echo ""
	echo "STS OK but S3 failed — attach s3:ListBucket, s3:PutObject, s3:GetObject on this bucket."
	exit 1
fi

echo ""
echo "AWS credentials and bucket list look OK."
