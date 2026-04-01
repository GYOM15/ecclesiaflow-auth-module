# CLAUDE.md — EcclesiaFlow Authentication Module

## Project Overview

Centralized authentication module for the EcclesiaFlow church management platform. Provides JWT-based authentication, password management, and inter-module communication via gRPC for a multi-tenant architecture (each church is an independent tenant).

- **Artifact**: `com.ecclesiaflow:ecclesiaflow-auth-module:1.0.0-SNAPSHOT`
- **Java**: 21
- **Spring Boot**: 3.5.5
- **Default port**: 8081

---

## Build & Run Commands

```bash
# Compile and generate OpenAPI + Protobuf sources
mvn clean generate-sources

# Run all tests (with JaCoCo coverage check — minimum 90% line & branch)
mvn test

# Run with coverage report
mvn clean test jacoco:report
open target/site/jacoco/index.html

# Start the application (dev)
mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081'

# Production JAR
mvn clean package
java -jar target/ecclesiaflow-auth-module-*.jar --spring.profiles.active=prod
```

**Note**: Generated sources must be marked as a source root in IntelliJ:
`target/generated-sources/openapi/src/main/java` → Mark as Generated Sources Root

---

## Architecture

### Layer Structure (Clean / Hexagonal Architecture)

```
com.ecclesiaflow.springsecurity/
├── application/          # Cross-cutting concerns (config, AOP, event handlers)
│   ├── config/           # Spring Security, gRPC, WebClient, Async, OpenAPI configs
│   ├── handlers/         # PasswordEventHandler (async email notifications)
│   └── logging/          # AOP aspects + SecurityMaskingUtils (PII/GDPR masking)
│
├── business/             # Pure business logic (no framework dependencies)
│   ├── domain/           # Domain objects + Port interfaces
│   │   ├── email/        # EmailClient (port)
│   │   ├── member/       # Member, Role, MemberRepository (port), MembersClient (port)
│   │   ├── password/     # SigninCredentials, PasswordManagement
│   │   ├── security/     # Scope (permission enum)
│   │   └── token/        # UserTokens, SetupToken, SetupTokenRepository (port)
│   ├── events/           # Domain events (PasswordSetEvent, PasswordChangedEvent, etc.)
│   ├── exceptions/       # EmailServiceException, GrpcCommunicationException
│   └── services/         # PasswordService, SetupTokenService + implementations
│
├── io/                   # Adapters (implements ports from business layer)
│   ├── email/            # EmailGrpcClient (implements EmailClient)
│   ├── members/          # MembersGrpcClient (implements MembersClient)
│   ├── grpc/server/      # AuthGrpcServiceImpl (gRPC server endpoint)
│   ├── keycloak/         # Keycloak Admin API integration (Feign clients)
│   └── persistence/      # JPA entities, mappers, repository implementations
│
└── web/                  # REST API layer
    ├── controller/        # PasswordController, CapabilitiesController
    ├── delegate/          # PasswordManagementDelegate, CapabilitiesDelegate
    ├── client/            # MembersClientImpl (WebClient fallback)
    ├── exception/         # InvalidRequestException, InvalidTokenException
    │   └── advice/        # GlobalExceptionHandler (@RestControllerAdvice)
    ├── mappers/           # OpenAPI model mappers
    ├── security/          # KeycloakJwtConverter
    └── constants/         # Messages
```

### Key Design Patterns

- **API-First**: OpenAPI spec (`src/main/resources/api/openapi.yaml`) is the source of truth. Never modify generated code in `target/generated-sources/`.
- **Delegate Pattern**: Controllers implement generated interfaces, delegate to `*Delegate` classes for business logic.
- **Ports & Adapters**: Business layer defines interfaces (ports); `io/` layer provides implementations (adapters).
- **Event-Driven**: Password operations publish domain events; `PasswordEventHandler` sends emails asynchronously.
- **gRPC Primary / WebClient Fallback**: Inter-module communication uses gRPC with HTTP fallback for resilience.

---

## API Contract

The OpenAPI spec is at `src/main/resources/api/openapi.yaml`.

**Key rule**: Always modify `openapi.yaml` first, then run `mvn generate-sources`. Never edit generated files.

| Endpoint | Auth |
|----------|------|
| `POST /ecclesiaflow/auth/password` | Bearer temp token |
| `POST /ecclesiaflow/auth/new-password` | Bearer access token |
| `POST /ecclesiaflow/auth/forgot-password` | None |
| `POST /ecclesiaflow/auth/reset-password` | Bearer temp token |

**Accept header versioning**: API uses `Accept` header for versioning.

---

## Inter-Module Communication

Three `.proto` files define gRPC contracts:
- `src/main/proto/auth_service.proto` — Auth gRPC server (JWT generation for Members)
- `src/main/proto/members_service.proto` — Auth as gRPC client to Members
- `src/main/proto/email_service.proto` — Auth as gRPC client to Email

Protobuf classes are generated automatically during `mvn generate-sources`.

---

## Security & Compliance Rules

**GDPR / PII**: Never log emails, passwords, tokens, or user IDs in plain text. Use `SecurityMaskingUtils` for masking. All aspects in `application/logging/` enforce this.

**Secrets**: All secrets come from environment variables (`.env` file via spring-dotenv). Never hardcode credentials. Check `application.properties` for the expected env var names.

**Token lifetimes**:
- Access token: 15 minutes
- Refresh token: 7 days
- Temporary/setup tokens: 15 minutes

**Authentication**: Keycloak as OAuth2 resource server. JWT validation via `spring-security-oauth2-resource-server`.

---

## Testing

- **Coverage requirement**: 90% line and branch coverage (enforced by JaCoCo at build time — `mvn test` will fail if below threshold).
- **Test DB**: H2 in-memory for unit/integration tests.
- **Excluded from coverage**: Generated OpenAPI/Protobuf code, Lombok-generated builders, simple exception classes, JPA entities.
- **Test libraries**: JUnit 5, Mockito (with mockito-inline for static mocking), Spring Security Test, LogCaptor.

```bash
# Run a specific test class
mvn test -Dtest="PasswordServiceImplTest"

# Run tests in a package
mvn test -Dtest="com.ecclesiaflow.springsecurity.web.security.*"
```

---

## Configuration

All environment variables are listed in `application.properties`. Required variables include:

```
SPRING_DATASOURCE_URL / USERNAME / PASSWORD / DRIVER_CLASS_NAME
KEYCLOAK_ISSUER_URI / KEYCLOAK_JWKS_URI
KEYCLOAK_ADMIN_SERVER_URL / REALM / SERVICE_CLIENT_ID / CLIENT_SECRET
GRPC_MEMBERS_HOST / PORT
GRPC_EMAIL_HOST / PORT
GRPC_SERVER_PORT / GRPC_SERVER_SHUTDOWN_TIMEOUT / GRPC_CLIENT_SHUTDOWN_TIMEOUT
CORS_ALLOWED_ORIGINS
AUTH_TOKEN_SETUP_TTL_HOURS / AUTH_PASSWORD_SETUP_ENDPOINT
ECCLESIAFLOW_MEMBERS_BASE_URL
```

Copy `.env.example` → `.env` and `application.properties.example` → `application.properties` before first run.

---

## Commit Convention

```
Type(scope): Description (≤50 chars, first letter capitalized)

Body (≤72 chars per line, if needed)

Types: Feat, Fix, Docs, Style, Refactor, Test, Chore
Scopes (optional): members, confirmation, email, persistence, web
```

Examples:
- `Feat(members): Add email validation service`
- `Fix: Correct passwordReset error message`
- `Refactor(persistence): Simplify SetupToken repository`

---

## PR Checklist

- [ ] `mvn test` passes (coverage ≥ 90%)
- [ ] No PII in logs (use `SecurityMaskingUtils`)
- [ ] `openapi.yaml` updated before any API change
- [ ] Generated sources not committed (`target/` is in `.gitignore`)
- [ ] No hardcoded secrets or infra details in code/logs
