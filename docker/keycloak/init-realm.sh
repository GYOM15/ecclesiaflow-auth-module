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
  -e "s|__GOOGLE_CLIENT_ID__|${GOOGLE_CLIENT_ID:-DISABLED}|g" \
  -e "s|__GOOGLE_CLIENT_SECRET__|${GOOGLE_CLIENT_SECRET:-DISABLED}|g" \
  -e "s|__FACEBOOK_CLIENT_ID__|${FACEBOOK_CLIENT_ID:-DISABLED}|g" \
  -e "s|__FACEBOOK_CLIENT_SECRET__|${FACEBOOK_CLIENT_SECRET:-DISABLED}|g" \
  -e "s|__SSL_REQUIRED__|${SSL_REQUIRED:-none}|g" \
  "$TEMPLATE" > "$OUTPUT"

# Verify no unresolved placeholders remain
if grep -q '__[A-Z0-9_]*__' "$OUTPUT"; then
  echo "[init-realm] ERROR: Unresolved placeholders found in generated realm:" >&2
  grep -o '__[A-Z0-9_]*__' "$OUTPUT" | sort -u >&2
  exit 1
fi

echo "[init-realm] Realm file generated: $OUTPUT"

# ---------------------------------------------------------------------------
# Start Keycloak
# ---------------------------------------------------------------------------
# In dev mode (start-dev): Keycloak runs in background so we can patch the
# master realm sslRequired after startup, then wait for the process.
# In production (start): straight exec — Keycloak becomes PID 1.
# ---------------------------------------------------------------------------

IS_DEV=false
case "$*" in *start-dev*) IS_DEV=true ;; esac

if [ "$IS_DEV" = "true" ]; then
  # --- DEV MODE -----------------------------------------------------------
  # Docker Desktop (macOS/Windows) port-maps host requests through the bridge
  # gateway (172.x.x.x), not 127.0.0.1. Combined with the master realm's
  # default sslRequired=external, this blocks HTTP access to the admin
  # console. We patch it to NONE after Keycloak starts.
  # -----------------------------------------------------------------------
  set +e

  /opt/keycloak/bin/kc.sh "$@" &
  KC_PID=$!

  trap "kill $KC_PID; wait $KC_PID; exit" INT TERM

  echo "[init-realm] Dev mode — waiting for Keycloak readiness (PID $KC_PID)..."
  ATTEMPTS=0
  while true; do
    if sh -c 'exec 3<>/dev/tcp/127.0.0.1/8080' 2>/dev/null; then
      break
    fi
    ATTEMPTS=$((ATTEMPTS + 1))
    if [ $ATTEMPTS -ge 60 ]; then
      echo "[init-realm] WARNING: Keycloak not ready after 120s — skipping master realm patch" >&2
      wait $KC_PID
      exit $?
    fi
    sleep 2
  done

  sleep 5

  echo "[init-realm] Patching master realm sslRequired=NONE..."
  if /opt/keycloak/bin/kcadm.sh config credentials \
       --server http://localhost:8080 \
       --realm master \
       --user "$KEYCLOAK_ADMIN" \
       --password "$KEYCLOAK_ADMIN_PASSWORD" 2>&1 \
     && /opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE 2>&1; then
    echo "[init-realm] Master realm sslRequired set to NONE (dev mode)"
  else
    echo "[init-realm] WARNING: Could not patch master realm sslRequired" >&2
  fi

  wait $KC_PID
else
  # --- PRODUCTION MODE ----------------------------------------------------
  # Straight exec — Keycloak becomes PID 1 for proper signal handling.
  # No master realm patch needed (production runs behind HTTPS proxy).
  # -----------------------------------------------------------------------
  echo "[init-realm] Production mode — starting Keycloak..."
  exec /opt/keycloak/bin/kc.sh "$@"
fi
