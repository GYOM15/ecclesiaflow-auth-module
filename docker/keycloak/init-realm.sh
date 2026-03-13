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
[ -z "${KEYCLOAK_FRONTEND_CLIENT_SECRET:-}" ]      && MISSING="$MISSING KEYCLOAK_FRONTEND_CLIENT_SECRET"
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
  -e "s|__KEYCLOAK_FRONTEND_CLIENT_SECRET__|${KEYCLOAK_FRONTEND_CLIENT_SECRET}|g" \
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

  # -----------------------------------------------------------------------
  # Create custom "social-auto-provision" authentication flow
  # -----------------------------------------------------------------------
  # Keycloak 23 replaces ALL built-in flows if authenticationFlows is in
  # the realm JSON, so we create the custom flow via Admin CLI instead.
  #
  # Flow logic:
  #   social-auto-provision (top-level)
  #     ├── idp-create-user-if-unique  (ALTERNATIVE) — new user → silent create
  #     └── social-auto-link           (ALTERNATIVE) — existing user → sub-flow
  #           ├── idp-detect-existing-broker-user (REQUIRED)
  #           └── idp-auto-link                   (REQUIRED)
  # -----------------------------------------------------------------------

  KCADM="/opt/keycloak/bin/kcadm.sh"
  REALM="ecclesiaflow"

  # Helper: extract execution ID by displayName from pretty-printed kcadm JSON.
  # Uses grep -B4 since "id" is always 2-4 lines before "displayName" in output.
  # Usage: get_exec_id "<json>" "<displayName>"
  get_exec_id() {
    echo "$1" | grep -B4 "\"$2\"" | grep '"id"' | tail -1 | sed 's/.*: "//;s/".*//'
  }

  # Helper: update execution requirement via raw PUT body (bypasses GET-merge-PUT).
  # Usage: set_req "<flow-alias>" "<exec-id>" "<requirement>"
  set_req() {
    printf '{"id":"%s","requirement":"%s"}' "$2" "$3" | \
      $KCADM update "authentication/flows/$1/executions" -r "$REALM" -f - 2>&1
  }

  echo "[init-realm] Creating social-auto-provision authentication flow..."

  # 1. Create the top-level flow
  $KCADM create authentication/flows \
    -r "$REALM" \
    -s alias=social-auto-provision \
    -s providerId=basic-flow \
    -s topLevel=true \
    -s builtIn=false \
    -s 'description=Silently create or link social login users — no forms shown' 2>&1

  # 2. Add "idp-create-user-if-unique" execution
  $KCADM create authentication/flows/social-auto-provision/executions/execution \
    -r "$REALM" \
    -s provider=idp-create-user-if-unique 2>&1

  # 3. Create "social-auto-link" sub-flow inside the top-level flow
  $KCADM create authentication/flows/social-auto-provision/executions/flow \
    -r "$REALM" \
    -s alias=social-auto-link \
    -s type=basic-flow \
    -s provider=registration-page-form \
    -s 'description=Auto-link social account to existing user by email' 2>&1

  # 4. Set requirements on the top-level flow executions
  TOP_EXECS=$($KCADM get authentication/flows/social-auto-provision/executions -r "$REALM" 2>/dev/null)

  EID=$(get_exec_id "$TOP_EXECS" "Create User If Unique")
  [ -n "$EID" ] && set_req "social-auto-provision" "$EID" "ALTERNATIVE" \
    && echo "[init-realm]   idp-create-user-if-unique → ALTERNATIVE"

  EID=$(get_exec_id "$TOP_EXECS" "social-auto-link")
  [ -n "$EID" ] && set_req "social-auto-provision" "$EID" "ALTERNATIVE" \
    && echo "[init-realm]   social-auto-link → ALTERNATIVE"

  # 5. Add executions to the sub-flow
  $KCADM create authentication/flows/social-auto-link/executions/execution \
    -r "$REALM" \
    -s provider=idp-detect-existing-broker-user 2>&1

  $KCADM create authentication/flows/social-auto-link/executions/execution \
    -r "$REALM" \
    -s provider=idp-auto-link 2>&1

  # 6. Set sub-flow executions to REQUIRED
  SUB_EXECS=$($KCADM get authentication/flows/social-auto-link/executions -r "$REALM" 2>/dev/null)

  EID=$(get_exec_id "$SUB_EXECS" "Detect existing broker user")
  [ -n "$EID" ] && set_req "social-auto-link" "$EID" "REQUIRED" \
    && echo "[init-realm]   idp-detect-existing-broker-user → REQUIRED"

  EID=$(get_exec_id "$SUB_EXECS" "Automatically set existing user")
  [ -n "$EID" ] && set_req "social-auto-link" "$EID" "REQUIRED" \
    && echo "[init-realm]   idp-auto-link → REQUIRED"

  # 7. Point identity providers to the new flow
  echo "[init-realm] Updating identity providers to use social-auto-provision..."

  $KCADM update identity-provider/instances/google \
    -r "$REALM" \
    -s firstBrokerLoginFlowAlias=social-auto-provision 2>&1 && \
    echo "[init-realm] Google IdP → social-auto-provision"

  $KCADM update identity-provider/instances/facebook \
    -r "$REALM" \
    -s firstBrokerLoginFlowAlias=social-auto-provision 2>&1 && \
    echo "[init-realm] Facebook IdP → social-auto-provision"

  echo "[init-realm] Social auto-provision flow configured"

  # -----------------------------------------------------------------------
  # Ensure identity_provider claim is in the frontend client JWT
  # -----------------------------------------------------------------------
  echo "[init-realm] Ensuring identity-provider-mapper on ecclesiaflow-frontend..."

  FRONTEND_CID=$($KCADM get clients -r "$REALM" -q clientId=ecclesiaflow-frontend --fields id 2>/dev/null \
    | grep '"id"' | sed 's/.*: "//;s/".*//')

  if [ -n "$FRONTEND_CID" ]; then
    # Check if mapper already exists (avoid duplicate on restart)
    EXISTING=$($KCADM get "clients/$FRONTEND_CID/protocol-mappers/models" -r "$REALM" 2>/dev/null \
      | grep '"identity-provider-mapper"' || true)

    if [ -z "$EXISTING" ]; then
      $KCADM create "clients/$FRONTEND_CID/protocol-mappers/models" \
        -r "$REALM" \
        -s name=identity-provider-mapper \
        -s protocol=openid-connect \
        -s protocolMapper=oidc-usersessionmodel-note-mapper \
        -s consentRequired=false \
        -s 'config."user.session.note"=identity_provider' \
        -s 'config."id.token.claim"=true' \
        -s 'config."access.token.claim"=true' \
        -s 'config."userinfo.token.claim"=false' \
        -s 'config."claim.name"=identity_provider' \
        -s 'config."jsonType.label"=String' 2>&1 && \
        echo "[init-realm]   identity-provider-mapper created" || \
        echo "[init-realm]   WARNING: Could not create identity-provider-mapper" >&2
    else
      echo "[init-realm]   identity-provider-mapper already exists — skipping"
    fi
  fi

  # -----------------------------------------------------------------------
  # Password policy: enforce strength + history
  # -----------------------------------------------------------------------
  echo "[init-realm] Setting password policy..."

  $KCADM update realms/"$REALM" \
    -s 'passwordPolicy=length(8) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and passwordHistory(3)' 2>&1 && \
    echo "[init-realm] Password policy: 8+ chars, mixed case, digit, special, history(3)"

  # -----------------------------------------------------------------------
  # Assign custom themes to the ecclesiaflow realm
  # -----------------------------------------------------------------------
  echo "[init-realm] Assigning custom themes..."

  $KCADM update realms/"$REALM" \
    -s loginTheme=ecclesiaflow-user \
    -s emailTheme=ecclesiaflow-base \
    -s resetPasswordAllowed=true 2>&1 && \
    echo "[init-realm] Realm themes: login=ecclesiaflow-user, email=ecclesiaflow-base"

  # Assign admin theme to the admin-service client (if needed)
  $KCADM update clients/"$($KCADM get clients -r "$REALM" -q clientId=ecclesiaflow-frontend --fields id 2>/dev/null | grep '"id"' | sed 's/.*: "//;s/".*//')" \
    -r "$REALM" \
    -s 'attributes.login_theme=ecclesiaflow-user' 2>&1 && \
    echo "[init-realm] ecclesiaflow-frontend client → ecclesiaflow-user theme"

  echo "[init-realm] Theme configuration complete"

  # -----------------------------------------------------------------------
  # Patch ecclesiaflow-frontend: confidential + Direct Grant
  # (--import-realm does NOT update existing clients, so we patch via CLI)
  # -----------------------------------------------------------------------
  FRONTEND_ID=$($KCADM get clients -r "$REALM" -q clientId=ecclesiaflow-frontend --fields id 2>/dev/null | grep '"id"' | sed 's/.*: "//;s/".*//')
  if [ -n "$FRONTEND_ID" ]; then
    echo "[init-realm] Patching ecclesiaflow-frontend → confidential + Direct Grant..."
    $KCADM update "clients/$FRONTEND_ID" -r "$REALM" \
      -s publicClient=false \
      -s clientAuthenticatorType=client-secret \
      -s "secret=${KEYCLOAK_FRONTEND_CLIENT_SECRET}" \
      -s directAccessGrantsEnabled=true 2>&1 && \
      echo "[init-realm] ecclesiaflow-frontend → confidential + Direct Grant enabled"
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
