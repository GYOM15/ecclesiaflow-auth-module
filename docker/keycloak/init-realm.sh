#!/bin/sh
# =============================================================================
# init-realm.sh
#
# Replaces __PLACEHOLDER__ variables in the realm template with actual values
# from environment variables, then starts Keycloak.
#
# Usage: Mounted as entrypoint in docker-compose. Keycloak arguments (e.g.
#        "start-dev --import-realm") are passed via CMD and forwarded with "$@".
# =============================================================================

set -eu

TEMPLATE="/opt/keycloak/data/template/realm-ecclesiaflow.json"
OUTPUT="/opt/keycloak/data/import/realm-ecclesiaflow.json"

# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------

if [ ! -f "$TEMPLATE" ]; then
  echo "[init-realm] ERROR: Template not found: $TEMPLATE" >&2
  exit 1
fi

MISSING=""
[ -z "${KEYCLOAK_BACKEND_CLIENT_SECRET:-}" ]      && MISSING="$MISSING KEYCLOAK_BACKEND_CLIENT_SECRET"
[ -z "${KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET:-}" ] && MISSING="$MISSING KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET"
[ -z "${KEYCLOAK_SMTP_PASSWORD:-}" ]               && MISSING="$MISSING KEYCLOAK_SMTP_PASSWORD"

if [ -n "$MISSING" ]; then
  echo "[init-realm] ERROR: Missing required environment variables:$MISSING" >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Generate realm file from template
# ---------------------------------------------------------------------------

mkdir -p "$(dirname "$OUTPUT")"

sed \
  -e "s|__KEYCLOAK_BACKEND_CLIENT_SECRET__|${KEYCLOAK_BACKEND_CLIENT_SECRET}|g" \
  -e "s|__KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET__|${KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET}|g" \
  -e "s|__FRONTEND_REDIRECT_URI_1__|${FRONTEND_REDIRECT_URI_1:-http://localhost:3000/*}|g" \
  -e "s|__FRONTEND_REDIRECT_URI_2__|${FRONTEND_REDIRECT_URI_2:-http://localhost:4200/*}|g" \
  -e "s|__FRONTEND_REDIRECT_URI_3__|${FRONTEND_REDIRECT_URI_3:-http://localhost:5173/*}|g" \
  -e "s|__FRONTEND_ORIGIN_1__|${FRONTEND_ORIGIN_1:-http://localhost:3000}|g" \
  -e "s|__FRONTEND_ORIGIN_2__|${FRONTEND_ORIGIN_2:-http://localhost:4200}|g" \
  -e "s|__FRONTEND_ORIGIN_3__|${FRONTEND_ORIGIN_3:-http://localhost:5173}|g" \
  -e "s|__KEYCLOAK_SMTP_FROM__|${KEYCLOAK_SMTP_FROM:-noreply@ecclesiaflow.com}|g" \
  -e "s|__KEYCLOAK_SMTP_USER__|${KEYCLOAK_SMTP_USER:-noreply@ecclesiaflow.com}|g" \
  -e "s|__KEYCLOAK_SMTP_PASSWORD__|${KEYCLOAK_SMTP_PASSWORD}|g" \
  "$TEMPLATE" > "$OUTPUT"

# Verify no unresolved placeholders remain
if grep -q '__[A-Z0-9_]*__' "$OUTPUT"; then
  echo "[init-realm] ERROR: Unresolved placeholders found in generated realm:" >&2
  grep -o '__[A-Z0-9_]*__' "$OUTPUT" | sort -u >&2
  exit 1
fi

echo "[init-realm] Realm file generated: $OUTPUT"

# ---------------------------------------------------------------------------
# Hand off to Keycloak (replaces this process — Keycloak becomes PID 1)
# ---------------------------------------------------------------------------

exec /opt/keycloak/bin/kc.sh "$@"
