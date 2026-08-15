#!/usr/bin/env bash
set -euo pipefail

BUCKET="${SUPABASE_STORAGE_BUCKET:-user-images}"
FILE_SIZE_LIMIT=5242880
MIME_TYPES='["image/jpeg","image/png","image/webp"]'

if [ -z "${SUPABASE_URL:-}" ] || [ -z "${SUPABASE_SERVICE_ROLE_KEY:-}" ]; then
  echo "Set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY before running this."
  echo "Both are on the project's API settings page. The service role key is a secret:"
  echo "export it in your shell, do not paste it into a file that git can see."
  exit 1
fi

API="${SUPABASE_URL%/}/storage/v1/bucket"
AUTH="Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY"
APIKEY="apikey: $SUPABASE_SERVICE_ROLE_KEY"

payload=$(cat <<EOF
{
  "id": "$BUCKET",
  "name": "$BUCKET",
  "public": false,
  "file_size_limit": $FILE_SIZE_LIMIT,
  "allowed_mime_types": $MIME_TYPES
}
EOF
)

existing=$(curl -sS -o /dev/null -w '%{http_code}' -H "$AUTH" -H "$APIKEY" "$API/$BUCKET")

if [ "$existing" = "200" ]; then
  echo "Bucket '$BUCKET' already exists, updating its limits."
  code=$(curl -sS -o /tmp/bucket-response.json -w '%{http_code}' -X PUT \
    -H "$AUTH" -H "$APIKEY" -H "Content-Type: application/json" \
    -d "$payload" "$API/$BUCKET")
else
  echo "Creating bucket '$BUCKET'."
  code=$(curl -sS -o /tmp/bucket-response.json -w '%{http_code}' -X POST \
    -H "$AUTH" -H "$APIKEY" -H "Content-Type: application/json" \
    -d "$payload" "$API")
fi

if [ "$code" != "200" ]; then
  echo "Storage API answered $code:"
  cat /tmp/bucket-response.json
  exit 1
fi

echo "Bucket '$BUCKET' is private, capped at $FILE_SIZE_LIMIT bytes, images only."
curl -sS -H "$AUTH" -H "$APIKEY" "$API/$BUCKET"
echo
