# Social Login Integration Guide

## Architecture Principles

| Layer | Responsibility | Stores profile data? |
|-------|---------------|---------------------|
| **Keycloak** | Authentication only (tokens, sessions, federated identities) | No — `firstName`/`lastName` from Google are a protocol side-effect, not the source of truth |
| **Auth Module** | Account lifecycle (password setup, reset, capabilities) | No |
| **Members Module** | User profile, church membership, business data | **Yes — source of truth** |
| **Frontend** | Orchestrates onboarding, reads profile from Members only | No |

The frontend **never** reads profile data from Keycloak. It always reads from the Members Module.

---

## Keycloak Client Configuration

The frontend uses the `ecclesiaflow-frontend` public client with PKCE (S256).

| Parameter | Value |
|-----------|-------|
| Client ID | `ecclesiaflow-frontend` |
| Flow | Authorization Code + PKCE (S256) |
| Public client | Yes (no client secret) |
| Token lifespan | 5 minutes (access), 7 days (refresh) |

### JWT Claims Available

| Claim | Source | Description |
|-------|--------|-------------|
| `sub` | Keycloak | Keycloak user ID (UUID) |
| `email` | Protocol mapper | User email |
| `roles` | Protocol mapper | Array of realm roles (e.g. `["USER"]`) |
| `preferred_username` | Keycloak default | Same as email (loginWithEmailAllowed) |

> **Note**: `given_name` and `family_name` may be present in the ID token for Google users, but the frontend should NOT rely on them for profile display. Always use the Members Module API.

### Keycloak Endpoints

| Action | Endpoint |
|--------|----------|
| Authorize | `GET /realms/ecclesiaflow/protocol/openid-connect/auth` |
| Token | `POST /realms/ecclesiaflow/protocol/openid-connect/token` |
| Refresh | `POST /realms/ecclesiaflow/protocol/openid-connect/token` (grant_type=refresh_token) |
| Logout | `POST /realms/ecclesiaflow/protocol/openid-connect/logout` |
| UserInfo | `GET /realms/ecclesiaflow/protocol/openid-connect/userinfo` |

---

## Flow 1 — New User via Google (first ever login)

The user has never used EcclesiaFlow. They click "Login with Google".

```
User          Frontend           Keycloak             Google            Members
 │               │                   │                   │                 │
 │─ Click        │                   │                   │                 │
 │  "Google" ───>│                   │                   │                 │
 │               │── Auth request ──>│                   │                 │
 │               │   (PKCE + scope)  │                   │                 │
 │               │                   │── Redirect ──────>│                 │
 │               │                   │   Google consent   │                 │
 │               │                   │                   │                 │
 │               │                   │<── Google token ──│                 │
 │               │                   │                   │                 │
 │               │                   │ social-auto-provision flow:         │
 │               │                   │ 1. idp-create-user-if-unique        │
 │               │                   │    → user NOT found → CREATE        │
 │               │                   │    → federated identity linked      │
 │               │                   │    → role USER assigned             │
 │               │                   │                   │                 │
 │               │<── JWT tokens ───│                   │                 │
 │               │   (access+refresh) │                   │                 │
 │               │                   │                   │                 │
 │               │── GET /members/me ──────────────────────────────────────>│
 │               │   Authorization: Bearer <token>       │                 │
 │               │<────────────────── 404 Not Found ─────────────────────│
 │               │                   │                   │                 │
 │<── Redirect   │                   │                   │                 │
 │   to onboard  │                   │                   │                 │
 │               │                   │                   │                 │
 │─ Fill form ──>│                   │                   │                 │
 │  (pre-filled) │── POST /members ───────────────────────────────────────>│
 │               │   { email, firstName, lastName, ... } │                 │
 │               │<────────────────── 201 Created ───────────────────────│
 │               │                   │                   │                 │
 │<── Dashboard  │                   │                   │                 │
```

### Frontend Steps

1. **Redirect to Keycloak** with `kc_idp_hint=google` to skip the Keycloak login page and go directly to Google:
   ```
   GET /realms/ecclesiaflow/protocol/openid-connect/auth
     ?client_id=ecclesiaflow-frontend
     &response_type=code
     &scope=openid email profile
     &redirect_uri=<your_callback_url>
     &code_challenge=<PKCE_challenge>
     &code_challenge_method=S256
     &kc_idp_hint=google
   ```
2. **Exchange code for tokens** at the token endpoint (standard OIDC Authorization Code + PKCE).
3. **Call `GET /members/me`** with the access token.
4. **If 404** → redirect to the onboarding page.
5. **Pre-fill the onboarding form** with data from the JWT `email` claim (and optionally `given_name`/`family_name` from the ID token for convenience).
6. **Submit onboarding** → `POST /members` with the user's profile data.
7. **Redirect to dashboard**.

---

## Flow 2 — Existing Google User Reconnects

The user has already logged in via Google before and has a Members profile.

```
User          Frontend           Keycloak             Google            Members
 │               │                   │                   │                 │
 │─ Click        │                   │                   │                 │
 │  "Google" ───>│                   │                   │                 │
 │               │── Auth request ──>│                   │                 │
 │               │                   │── Redirect ──────>│                 │
 │               │                   │<── Google token ──│                 │
 │               │                   │                   │                 │
 │               │                   │ Federated identity exists           │
 │               │                   │ → authenticate directly             │
 │               │                   │ → NO first-broker-login flow        │
 │               │                   │                   │                 │
 │               │<── JWT tokens ───│                   │                 │
 │               │                   │                   │                 │
 │               │── GET /members/me ──────────────────────────────────────>│
 │               │<────────────────── 200 OK ────────────────────────────│
 │               │                   │                   │                 │
 │<── Dashboard  │                   │                   │                 │
```

No special handling. The `social-auto-provision` flow is **not triggered** on subsequent logins — Keycloak recognizes the federated identity and authenticates directly.

---

## Flow 3 — Platform User Links Google Account

A user who registered via the normal signup flow (email + password) clicks "Login with Google" for the first time. Their email in Keycloak matches their Google email.

```
User          Frontend           Keycloak             Google            Members
 │               │                   │                   │                 │
 │─ Click        │                   │                   │                 │
 │  "Google" ───>│                   │                   │                 │
 │               │── Auth request ──>│                   │                 │
 │               │                   │── Redirect ──────>│                 │
 │               │                   │<── Google token ──│                 │
 │               │                   │                   │                 │
 │               │                   │ social-auto-provision flow:         │
 │               │                   │ 1. idp-create-user-if-unique        │
 │               │                   │    → user EXISTS (same email)       │
 │               │                   │    → attempted()                    │
 │               │                   │ 2. social-auto-link sub-flow        │
 │               │                   │    → detect-existing-broker-user    │
 │               │                   │    → auto-link Google identity      │
 │               │                   │                   │                 │
 │               │<── JWT tokens ───│                   │                 │
 │               │                   │                   │                 │
 │               │── GET /members/me ──────────────────────────────────────>│
 │               │<────────────────── 200 OK ────────────────────────────│
 │               │                   │                   │                 │
 │<── Dashboard  │                   │                   │                 │
```

The auto-link is transparent. The user now has both login methods (email/password AND Google). Their Members profile already exists, so `GET /members/me` returns 200.

---

## Frontend Decision Logic

After receiving tokens from Keycloak (regardless of login method):

```
┌─────────────────────────────┐
│ Tokens received from        │
│ Keycloak (access + refresh) │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ GET /members/me             │
│ Authorization: Bearer token │
└──────────────┬──────────────┘
               │
        ┌──────┴──────┐
        │             │
     200 OK       404 Not Found
        │             │
        ▼             ▼
┌──────────────┐ ┌───────────────────┐
│  Dashboard   │ │  Onboarding page  │
│  (existing   │ │  (new user needs  │
│   user)      │ │   to complete     │
│              │ │   their profile)  │
└──────────────┘ └────────┬──────────┘
                          │
                          ▼
                 ┌───────────────────┐
                 │ POST /members     │
                 │ { profile data }  │
                 └────────┬──────────┘
                          │
                       201 Created
                          │
                          ▼
                 ┌───────────────────┐
                 │  Dashboard        │
                 └───────────────────┘
```

This logic is **identical** for email/password login and social login. The frontend does not need to know HOW the user authenticated.

---

## Onboarding Page Specification

### When to show

Show the onboarding page when `GET /members/me` returns **404**.

### Pre-filled fields

Extract from the JWT (ID token or access token):

| Field | JWT claim | Notes |
|-------|-----------|-------|
| Email | `email` | Read-only — comes from Keycloak, cannot be changed |
| First name | `given_name` (if present) | Editable — only a suggestion from Google |
| Last name | `family_name` (if present) | Editable — only a suggestion from Google |

### Required fields (from Members Module)

The exact fields depend on the Members Module API contract. At minimum:
- Email (from JWT)
- First name
- Last name
- Any other fields required by `POST /members`

### Submit

```
POST /members
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "email": "user@gmail.com",
  "firstName": "John",
  "lastName": "Doe",
  ... (other required fields)
}
```

---

## Login Button Implementation

### Direct Google Login (recommended)

Use `kc_idp_hint=google` to bypass the Keycloak login page entirely:

```
GET /realms/ecclesiaflow/protocol/openid-connect/auth
  ?client_id=ecclesiaflow-frontend
  &response_type=code
  &scope=openid email profile
  &redirect_uri=https://app.ecclesiaflow.com/callback
  &code_challenge=<...>
  &code_challenge_method=S256
  &kc_idp_hint=google
```

### Keycloak Login Page (shows all options)

Omit `kc_idp_hint` to show Keycloak's login page with email/password form AND social login buttons:

```
GET /realms/ecclesiaflow/protocol/openid-connect/auth
  ?client_id=ecclesiaflow-frontend
  &response_type=code
  &scope=openid email profile
  &redirect_uri=https://app.ecclesiaflow.com/callback
  &code_challenge=<...>
  &code_challenge_method=S256
```

### Recommended UI

```
┌──────────────────────────────┐
│        Se connecter          │
│                              │
│  ┌────────────────────────┐  │
│  │  G  Continuer avec     │  │
│  │     Google              │  │
│  └────────────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │  f  Continuer avec     │  │
│  │     Facebook            │  │
│  └────────────────────────┘  │
│                              │
│  ──────── ou ────────        │
│                              │
│  Email    [______________]   │
│  Mot de   [______________]   │
│  passe                       │
│                              │
│  ┌────────────────────────┐  │
│  │     Se connecter        │  │
│  └────────────────────────┘  │
│                              │
│  Mot de passe oublié ?       │
│  Pas de compte ? S'inscrire  │
└──────────────────────────────┘
```

- "Continuer avec Google" → auth URL with `kc_idp_hint=google`
- "Continuer avec Facebook" → auth URL with `kc_idp_hint=facebook`
- "Se connecter" (email/password) → auth URL without `kc_idp_hint`
- "S'inscrire" → platform self-registration flow (Members Module)

---

## Error Handling

| Scenario | HTTP Response | Frontend Action |
|----------|--------------|-----------------|
| Google login success, new user | `GET /members/me` → 404 | Redirect to onboarding |
| Google login success, existing user | `GET /members/me` → 200 | Redirect to dashboard |
| Google login cancelled by user | Keycloak error callback | Show "Login cancelled" message |
| Google account email mismatch | Should not happen (trustEmail=true) | Show generic error |
| Token expired during onboarding | `POST /members` → 401 | Refresh token, retry |
| Members service unavailable | `GET /members/me` → 503 | Show "Service temporarily unavailable", retry |

---

## Security Considerations

1. **PKCE is mandatory** — the frontend client is configured with `pkce.code.challenge.method=S256`. The authorization request MUST include `code_challenge` and `code_challenge_method`.
2. **Token storage** — store tokens in memory (not localStorage). Use refresh tokens for session persistence.
3. **Email trust** — `trustEmail=true` on Google/Facebook IdPs means Keycloak does not require email verification for social login users. Google/Facebook have already verified the email.
4. **No client secret** — `ecclesiaflow-frontend` is a public client. Authentication relies on PKCE, not a shared secret.
5. **Redirect URIs** — only whitelisted URIs in the Keycloak client config will be accepted. Configure them via `__FRONTEND_REDIRECT_URI_*__` placeholders.

---

## Keycloak Authentication Flow (reference)

This flow is automatically configured by `init-realm.sh` on every `docker compose up`:

```
social-auto-provision (top-level)
  ├── idp-create-user-if-unique  (ALTERNATIVE)
  │     New user → silently create account + link identity
  │
  └── social-auto-link           (ALTERNATIVE, sub-flow)
        ├── idp-detect-existing-broker-user (REQUIRED)
        │     Find existing Keycloak user by email
        │
        └── idp-auto-link                   (REQUIRED)
              Link social identity to existing account
```

- The flow is assigned to Google and Facebook IdPs via `firstBrokerLoginFlowAlias`.
- It only triggers on the **first** social login. Subsequent logins use the linked federated identity directly.
- `syncMode: FORCE` ensures Google attributes are synced on every login (Keycloak-side only, not authoritative).
