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
  -e "s|__FRONTEND_POST_LOGOUT_REDIRECT_URI__|${FRONTEND_POST_LOGOUT_REDIRECT_URI:-http://localhost:3000}|g" \
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
      -s directAccessGrantsEnabled=true \
      -s "attributes.post.logout.redirect.uris=${FRONTEND_POST_LOGOUT_REDIRECT_URI:-http://localhost:3000}" 2>&1 && \
      echo "[init-realm] ecclesiaflow-frontend → confidential + Direct Grant + post-logout URI"
  fi

  wait $KC_PID
else
  # --- PRODUCTION MODE ----------------------------------------------------
  # Start Keycloak in background, wait for readiness, then configure via
  # REST API (kcadm.sh silently fails in prod mode on Keycloak 23).
  # -----------------------------------------------------------------------
  echo "[init-realm] Production mode — starting Keycloak..."

  /opt/keycloak/bin/kc.sh "$@" &
  KC_PID=$!

  trap "kill $KC_PID; wait $KC_PID; exit" INT TERM

  echo "[init-realm] Waiting for Keycloak readiness..."
  ATTEMPTS=0
  while true; do
    if sh -c 'exec 3<>/dev/tcp/127.0.0.1/8080' 2>/dev/null; then
      break
    fi
    ATTEMPTS=$((ATTEMPTS + 1))
    if [ $ATTEMPTS -ge 90 ]; then
      echo "[init-realm] WARNING: Keycloak not ready after 180s — skipping post-config" >&2
      wait $KC_PID
      exit $?
    fi
    sleep 2
  done

  sleep 5
  echo "[init-realm] Keycloak is ready — running post-startup configuration via REST API..."

  # -----------------------------------------------------------------------
  # Get admin token
  # -----------------------------------------------------------------------
  KC_URL="http://localhost:8080"
  ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
  ADMIN_PASS="${KEYCLOAK_ADMIN_PASSWORD}"
  REALM="ecclesiaflow"

  get_token() {
    curl -sf -X POST "$KC_URL/realms/master/protocol/openid-connect/token" \
      -d "client_id=admin-cli" \
      -d "username=$ADMIN_USER" \
      -d "password=$ADMIN_PASS" \
      -d "grant_type=password" 2>/dev/null | sed 's/.*"access_token":"//;s/".*//'
  }

  TOKEN=$(get_token)
  if [ -z "$TOKEN" ]; then
    echo "[init-realm] WARNING: Could not get admin token — skipping post-config" >&2
    wait $KC_PID
    exit $?
  fi

  # Helper for authenticated API calls
  kc_put() {
    curl -sf -X PUT "$KC_URL$1" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "$2" 2>/dev/null
  }

  kc_get() {
    curl -sf "$KC_URL$1" -H "Authorization: Bearer $TOKEN" 2>/dev/null
  }

  # -----------------------------------------------------------------------
  # 1. SMTP Configuration
  # -----------------------------------------------------------------------
  echo "[init-realm] Configuring SMTP..."
  kc_put "/admin/realms/$REALM" "{\"smtpServer\":{\"host\":\"smtp.gmail.com\",\"port\":\"587\",\"starttls\":\"true\",\"auth\":\"true\",\"from\":\"${KEYCLOAK_SMTP_FROM:-noreply@ecclesiaflow.com}\",\"user\":\"${KEYCLOAK_SMTP_USER}\",\"password\":\"${KEYCLOAK_SMTP_PASSWORD}\"}}" \
    && echo "[init-realm]   SMTP configured" \
    || echo "[init-realm]   WARNING: SMTP configuration failed" >&2

  # -----------------------------------------------------------------------
  # 2. Google OAuth IdP (skip if DISABLED)
  # -----------------------------------------------------------------------
  if [ "${GOOGLE_CLIENT_ID:-DISABLED}" != "DISABLED" ] && [ -n "${GOOGLE_CLIENT_SECRET:-}" ]; then
    echo "[init-realm] Configuring Google IdP..."
    kc_put "/admin/realms/$REALM/identity-provider/instances/google" \
      "{\"alias\":\"google\",\"providerId\":\"google\",\"enabled\":true,\"trustEmail\":true,\"config\":{\"clientId\":\"${GOOGLE_CLIENT_ID}\",\"clientSecret\":\"${GOOGLE_CLIENT_SECRET}\",\"defaultScope\":\"openid email profile\",\"syncMode\":\"FORCE\",\"useJwksUrl\":\"true\"}}" \
      && echo "[init-realm]   Google IdP configured" \
      || echo "[init-realm]   WARNING: Google IdP configuration failed" >&2
  else
    echo "[init-realm]   Google IdP skipped (credentials not provided)"
  fi

  # -----------------------------------------------------------------------
  # 3. PKCE + Post-logout redirect URIs on frontend client
  # -----------------------------------------------------------------------
  echo "[init-realm] Configuring frontend client..."
  FRONTEND_CID=$(kc_get "/admin/realms/$REALM/clients?clientId=ecclesiaflow-frontend" \
    | sed 's/.*"id":"//' | sed 's/".*//')

  if [ -n "$FRONTEND_CID" ]; then
    kc_put "/admin/realms/$REALM/clients/$FRONTEND_CID" \
      "{\"publicClient\":false,\"clientAuthenticatorType\":\"client-secret\",\"secret\":\"${KEYCLOAK_FRONTEND_CLIENT_SECRET}\",\"directAccessGrantsEnabled\":true,\"attributes\":{\"pkce.code.challenge.method\":\"\",\"post.logout.redirect.uris\":\"${FRONTEND_REDIRECT_URI_1:-https://app.gyom-tech.com/*}\"}}" \
      && echo "[init-realm]   Frontend client: confidential + Direct Grant + PKCE disabled + post-logout URI" \
      || echo "[init-realm]   WARNING: Frontend client configuration failed" >&2
  fi

  # -----------------------------------------------------------------------
  # 4. Themes
  # -----------------------------------------------------------------------
  echo "[init-realm] Assigning themes..."
  kc_put "/admin/realms/$REALM" \
    "{\"loginTheme\":\"ecclesiaflow-user\",\"emailTheme\":\"ecclesiaflow-base\",\"resetPasswordAllowed\":true}" \
    && echo "[init-realm]   Realm themes: login=ecclesiaflow-user, email=ecclesiaflow-base" \
    || echo "[init-realm]   WARNING: Theme assignment failed" >&2

  # -----------------------------------------------------------------------
  # 5. Password policy
  # -----------------------------------------------------------------------
  echo "[init-realm] Setting password policy..."
  kc_put "/admin/realms/$REALM" \
    "{\"passwordPolicy\":\"length(8) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and passwordHistory(3)\"}" \
    && echo "[init-realm]   Password policy: 8+ chars, mixed case, digit, special, history(3)" \
    || echo "[init-realm]   WARNING: Password policy failed" >&2

  # -----------------------------------------------------------------------
  # 6. Social auto-provision flow (via kcadm — flow creation not in REST)
  # -----------------------------------------------------------------------
  KCADM="/opt/keycloak/bin/kcadm.sh"

  $KCADM config credentials --server "$KC_URL" --realm master \
    --user "$ADMIN_USER" --password "$ADMIN_PASS" 2>&1

  # Check if flow already exists
  FLOW_EXISTS=$($KCADM get authentication/flows -r "$REALM" 2>/dev/null \
    | grep '"social-auto-provision"' || true)

  if [ -z "$FLOW_EXISTS" ]; then
    echo "[init-realm] Creating social-auto-provision flow..."

    get_exec_id() {
      echo "$1" | grep -B4 "\"$2\"" | grep '"id"' | tail -1 | sed 's/.*: "//;s/".*//'
    }

    set_req() {
      printf '{"id":"%s","requirement":"%s"}' "$2" "$3" | \
        $KCADM update "authentication/flows/$1/executions" -r "$REALM" -f - 2>&1
    }

    $KCADM create authentication/flows -r "$REALM" \
      -s alias=social-auto-provision -s providerId=basic-flow \
      -s topLevel=true -s builtIn=false \
      -s 'description=Silently create or link social login users' 2>&1

    $KCADM create authentication/flows/social-auto-provision/executions/execution \
      -r "$REALM" -s provider=idp-create-user-if-unique 2>&1

    $KCADM create authentication/flows/social-auto-provision/executions/flow \
      -r "$REALM" -s alias=social-auto-link -s type=basic-flow \
      -s provider=registration-page-form \
      -s 'description=Auto-link social account to existing user by email' 2>&1

    TOP_EXECS=$($KCADM get authentication/flows/social-auto-provision/executions -r "$REALM" 2>/dev/null)
    EID=$(get_exec_id "$TOP_EXECS" "Create User If Unique")
    [ -n "$EID" ] && set_req "social-auto-provision" "$EID" "ALTERNATIVE"
    EID=$(get_exec_id "$TOP_EXECS" "social-auto-link")
    [ -n "$EID" ] && set_req "social-auto-provision" "$EID" "ALTERNATIVE"

    $KCADM create authentication/flows/social-auto-link/executions/execution \
      -r "$REALM" -s provider=idp-detect-existing-broker-user 2>&1
    $KCADM create authentication/flows/social-auto-link/executions/execution \
      -r "$REALM" -s provider=idp-auto-link 2>&1

    SUB_EXECS=$($KCADM get authentication/flows/social-auto-link/executions -r "$REALM" 2>/dev/null)
    EID=$(get_exec_id "$SUB_EXECS" "Detect existing broker user")
    [ -n "$EID" ] && set_req "social-auto-link" "$EID" "REQUIRED"
    EID=$(get_exec_id "$SUB_EXECS" "Automatically set existing user")
    [ -n "$EID" ] && set_req "social-auto-link" "$EID" "REQUIRED"

    echo "[init-realm]   social-auto-provision flow created"
  else
    echo "[init-realm]   social-auto-provision flow already exists — skipping"
  fi

  # Point Google IdP to social-auto-provision
  if [ "${GOOGLE_CLIENT_ID:-DISABLED}" != "DISABLED" ]; then
    $KCADM update identity-provider/instances/google -r "$REALM" \
      -s firstBrokerLoginFlowAlias=social-auto-provision 2>&1 \
      && echo "[init-realm]   Google IdP → social-auto-provision"
  fi

  # -----------------------------------------------------------------------
  # 7. Identity provider mapper on frontend client
  # -----------------------------------------------------------------------
  if [ -n "$FRONTEND_CID" ]; then
    MAPPER_EXISTS=$(kc_get "/admin/realms/$REALM/clients/$FRONTEND_CID/protocol-mappers/models" \
      | grep '"identity-provider-mapper"' || true)

    if [ -z "$MAPPER_EXISTS" ]; then
      echo "[init-realm] Creating identity-provider-mapper..."
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
        -s 'config."jsonType.label"=String' 2>&1 \
        && echo "[init-realm]   identity-provider-mapper created" \
        || echo "[init-realm]   WARNING: Could not create identity-provider-mapper" >&2
    else
      echo "[init-realm]   identity-provider-mapper already exists — skipping"
    fi
  fi

  echo "[init-realm] Post-startup configuration complete"

  wait $KC_PID
fi
