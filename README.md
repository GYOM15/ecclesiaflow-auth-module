# EcclesiaFlow Authentication Module

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot 3.5.5](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Keycloak](https://img.shields.io/badge/Keycloak-23.0-blue.svg)](https://www.keycloak.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Centralized authentication module for the **EcclesiaFlow** multi-tenant church management
platform. It delegates identity management (login, tokens, roles) to **Keycloak** and
exposes REST endpoints for initial password setup and platform capabilities.

---

## Tech Stack

| Layer          | Technology                                       |
|----------------|--------------------------------------------------|
| Language       | [Java 21](https://openjdk.java.net/projects/jdk/21/) |
| Framework      | [Spring Boot 3.5.5](https://spring.io/projects/spring-boot), [Spring Security 6](https://docs.spring.io/spring-security/reference/) (OAuth2 Resource Server) |
| Identity       | [Keycloak 23.0](https://www.keycloak.org/) (OAuth2 / OIDC) |
| Keycloak Admin | [OpenFeign](https://spring.io/projects/spring-cloud-openfeign) (service-account client credentials) |
| Inter-module   | [gRPC 1.65.1](https://grpc.io/) / [Protobuf 4.28.2](https://protobuf.dev/) |
| Resilience     | [Spring Retry](https://github.com/spring-projects/spring-retry) (compensation on partial failures) |
| Persistence    | [MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/), [JPA](https://jakarta.ee/specifications/persistence/), [MapStruct](https://mapstruct.org/) |
| API-first      | [OpenAPI Generator 7.15.0](https://openapi-generator.tech/) |
| Quality        | [JaCoCo](https://www.jacoco.org/jacoco/) (90 % minimum coverage) |

---

## Architecture

The module follows **Clean Architecture** with Ports & Adapters.
Business logic has zero framework dependency; all I/O goes through ports.

```
src/main/java/com/ecclesiaflow/springsecurity/
├── application/               # Spring config, event handlers, AOP logging
│   ├── config/                # Security, gRPC, Retry, Async, OpenAPI
│   ├── handlers/              # PasswordEventHandler (async domain events)
│   └── logging/               # Structured logging aspects (per layer)
├── business/                  # Pure domain — no Spring imports
│   ├── domain/                # SetupToken, PasswordManagement, MembersClient (port)
│   ├── services/              # PasswordService, SetupTokenService
│   ├── events/                # PasswordSetEvent
│   └── exceptions/            # CompensationFailedException, InvalidTokenException
├── io/                        # Infrastructure adapters
│   ├── keycloak/              # KeycloakAdminClient, Feign clients, DTOs
│   ├── grpc/server/           # AuthGrpcServiceImpl (inbound gRPC)
│   ├── members/               # MembersGrpcClient (outbound gRPC, implements port)
│   └── persistence/           # JPA entities, MapStruct mappers, repositories
└── web/                       # REST layer
    ├── controller/            # PasswordController, CapabilitiesController
    ├── delegate/              # Delegates (orchestrate service calls)
    ├── exception/             # GlobalExceptionHandler, ApiErrorResponse
    └── security/              # KeycloakJwtConverter
```

### Request Flow (Password Setup)

```
Client ──POST──▸ PasswordController
                   └─▸ PasswordManagementDelegate
                         ├─▸ SetupTokenService   (validate & consume token)
                         ├─▸ KeycloakAdminClient  (create user in Keycloak)
                         ├─▸ MembersGrpcClient    (confirm to Members module)
                         └─▸ PasswordSetEvent     (async post-processing)
                   ◂── 201 Created
```

If the Keycloak or gRPC call fails after a partial write, **Spring Retry** retries the
operation and a **compensation** mechanism rolls back the previous steps.

### Keycloak Roles

| Role          | Description                                |
|---------------|--------------------------------------------|
| `SUPER_ADMIN` | Platform administrator — full access       |
| `SUPPORT`     | Support staff — read-only access           |
| `USER`        | Verified account (not yet a church member) |

Church-specific roles are managed by the Church Module, not by this service.

---

## Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **MySQL 8.0+**
- **Docker** (for Keycloak)

---

## Quick Start

### 1. Start Keycloak

```bash
cp docker/.env.example docker/.env
# Fill in the required secrets (see comments in .env.example)
docker compose -f docker/docker-compose.keycloak.yml --env-file docker/.env up -d
```

> The `init-realm.sh` script automatically replaces `__PLACEHOLDER__` variables in the
> realm JSON with the actual secrets from your `docker/.env` at container startup.
> No manual editing of the realm file is needed.

### 2. Configure the application

```bash
cp .env.example .env
```

Key variables to fill in:

| Variable                              | Description                          |
|---------------------------------------|--------------------------------------|
| `SPRING_DATASOURCE_URL`              | JDBC URL to MySQL                    |
| `SPRING_DATASOURCE_USERNAME`         | Database user                        |
| `SPRING_DATASOURCE_PASSWORD`         | Database password                    |
| `KEYCLOAK_ISSUER_URI`               | Keycloak realm issuer URL            |
| `KEYCLOAK_ADMIN_SERVER_URL`         | Keycloak base URL                    |
| `KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET` | Service-account client secret     |
| `GRPC_MEMBERS_HOST` / `GRPC_MEMBERS_PORT` | Members module gRPC address   |

See `.env.example` for the full list with defaults.

### 3. Build and run

```bash
mvn clean verify          # compile + tests + coverage check
mvn spring-boot:run       # start on port 8081
```

The application is available at `http://localhost:8081`.

---

## Docker (Keycloak)

The `docker/` directory contains a fully automated Keycloak setup:

| File | Purpose |
|------|----------|
| `docker-compose.keycloak.yml` | Keycloak 23.0 + PostgreSQL 16 |
| `keycloak/realm-ecclesiaflow.json` | Realm template with `__PLACEHOLDER__` tokens |
| `keycloak/init-realm.sh` | Entrypoint script — injects secrets at startup |
| `.env.example` | Documented environment variables template |

**Environment variables injected by `init-realm.sh`:**

| Variable | Required | Description |
|----------|----------|-------------|
| `KEYCLOAK_BACKEND_CLIENT_SECRET` | Yes | OAuth2 backend client secret |
| `KEYCLOAK_ADMIN_SERVICE_CLIENT_SECRET` | Yes | Admin service-account secret |
| `KEYCLOAK_SMTP_PASSWORD` | Yes | Gmail App Password for email features |
| `KEYCLOAK_SMTP_FROM` / `KEYCLOAK_SMTP_USER` | No | Defaults to `noreply@ecclesiaflow.com` |
| `FRONTEND_REDIRECT_URI_*` / `FRONTEND_ORIGIN_*` | No | Defaults to `localhost` dev ports |

---

## gRPC Services

The module communicates with other EcclesiaFlow modules via gRPC.
Proto files are in `src/main/proto/`.

### Inbound (this module exposes)

| Proto | Service | RPC | Called by |
|-------|---------|-----|-----------|
| `auth_service.proto` | `AuthService` | `GenerateTemporaryToken` | Members module |

### Outbound (this module calls)

| Proto | Service | RPC | Purpose |
|-------|---------|-----|---------|
| `members_service.proto` | `MembersService` | `GetMemberConfirmationStatus` | Verify email is confirmed |
| `members_service.proto` | `MembersService` | `NotifyAccountActivated` | Confirm Keycloak user creation |

---

## API

### Endpoints

| Method | Path                                  | Auth           | Description                                  |
|--------|---------------------------------------|----------------|----------------------------------------------|
| POST   | `/ecclesiaflow/auth/password/setup`   | `X-Setup-Token` header | Initial password setup (creates Keycloak user) |
| GET    | `/ecclesiaflow/capabilities`          | Bearer JWT     | All platform capabilities (SUPER_ADMIN, SUPPORT) |
| GET    | `/ecclesiaflow/capabilities/auth`     | Bearer JWT     | Auth module capabilities (SUPER_ADMIN, SUPPORT)  |

### Password Setup

```bash
curl -X POST http://localhost:8081/ecclesiaflow/auth/password/setup \
  -H "Content-Type: application/json" \
  -H "X-Setup-Token: <token-received-during-onboarding>" \
  -d '{"password": "SecurePassword123!"}'
```

**Response** `201 Created`:

```json
{
  "status": "PASSWORD_SET",
  "message": "Password successfully configured"
}
```

### Error Responses

| Code | Reason                                          |
|------|-------------------------------------------------|
| 400  | Invalid request body or password policy violation |
| 401  | Missing or invalid setup token / JWT             |
| 403  | Insufficient role for capabilities endpoints     |
| 409  | Password already set for this token              |
| 500  | Internal error (Keycloak unreachable, etc.)      |

### API Documentation

- **Swagger UI** — http://localhost:8081/swagger-ui/index.html
- **OpenAPI JSON** — http://localhost:8081/v3/api-docs
- **Contract source** — `src/main/resources/api/openapi.yaml`

---

## Testing

The project has **30 test classes** covering all layers:

```bash
mvn verify                          # unit + integration tests + 90% coverage gate
open target/site/jacoco/index.html  # HTML coverage report
```

| Category      | Scope                                            |
|---------------|--------------------------------------------------|
| Unit          | Domain objects, services, delegates, controllers |
| Integration   | gRPC clients, Security configuration, MapStruct  |
| AOP           | Logging aspects (per layer)                      |
| Exception     | GlobalExceptionHandler, error models             |

---

## CI/CD

GitHub Actions runs on every push and on pull requests to `main` and `dev`:

- Checkout → JDK 21 setup → Maven cache → `mvn verify`

Workflow file: `.github/workflows/ecclesiaflow-auth-module-ci.yaml`

---

## Contributing

1. Create a feature branch from `main`
2. Follow commit conventions in [COMMIT_CONVENTION.md](COMMIT_CONVENTION.md)
3. Ensure `mvn verify` passes (tests + 90 % coverage)
4. Open a Pull Request

---

## Related Modules

| Module | Description |
|--------|-------------|
| [ecclesiaflow-members-module](https://github.com/GYOM15/ecclesiaflow-members-module) | Member registration, email confirmation, church management |
| [ecclesiaflow-communication-module](https://github.com/GYOM15/ecclesiaflow-communication-module) | Transactional email service (RabbitMQ, templates) |

---

## License

MIT License — see [LICENSE](LICENSE) for details.