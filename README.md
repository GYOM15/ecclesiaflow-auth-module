# 🔐 EcclesiaFlow Authentication Module

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![gRPC](https://img.shields.io/badge/gRPC-1.60.0-blue.svg)](https://grpc.io/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.5.x-6DB33F.svg)](https://spring.io/projects/spring-security)
> **Centralized authentication module for the EcclesiaFlow church management platform**

A robust and secure authentication service designed to support EcclesiaFlow's multi-tenant architecture, where each church constitutes an independent tenant with its own administrator (pastor) and members. Automatic API generation via OpenAPI Generator with Delegate pattern.

## Table of Contents

- [Overview](#-overview)
- [Architecture](#️-architecture)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Configuration](#-configuration)
- [Security](#️-security)
- [Tests](#-tests)
- [Deployment](#-deployment)
- [Contribution](#-contribution)

## Overview

### Module Objective

This module provides centralized authentication services for the EcclesiaFlow ecosystem:

- **JWT token generation** (access + refresh) for resource access
- **Automatic refresh** of expired tokens
- **Password management** (initial setup + change + forgot/reset)
- **Temporary tokens** for registration and password reset
- **Event-driven architecture** for asynchronous operations
- **Email notifications** via gRPC delegation to Email module
- **Granular scope system** for permissions
- **Multi-tenant support** ready for distributed architecture
- **API-First Design** with automatic generation via OpenAPI Generator
- **gRPC tri-directional communication** (Auth ↔ Members ↔ Email)
- **Resilient inter-module communication** with WebClient fallback

```mermaid
sequenceDiagram
    participant Members as Members Module
    participant Auth as Auth Module
    participant Email as Email Module

    Note over Members,Auth: 1. Generate Temporary Token (gRPC Server)
    Members->>Auth: GenerateTemporaryToken(email, memberId)
    Auth->>Auth: Create JWT (purpose: password_setup)
    Auth-->>Members: Return temporary token

    Note over Auth: 2. Password Setup (REST API)
    Auth->>Auth: Receive POST /auth/password<br/>(Bearer temporaryToken)
    Auth->>Auth: Validate token purpose
    Auth->>Members: GetMemberConfirmationStatus (gRPC Client)
    Members-->>Auth: Return confirmation status
    Auth->>Auth: Set password + enabled=true
    Auth->>Auth: Publish PasswordSetEvent
    Auth->>Auth: Generate access/refresh tokens
    Auth-->>Auth: Return {accessToken, refreshToken}

    Note over Auth,Email: 3. Async Email Notification (Event Handler)
    Auth->>Email: Send welcome email (gRPC)
```
### Auth Module Responsibilities
- **Generate temporary JWT tokens** (gRPC server for Members)
- **Manage password operations** (set, change, reset)
- **Validate member status** (gRPC client to Members)
- **Trigger email notifications** (event-driven, async)
- **Issue authentication tokens** (access + refresh)


### Target Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SUPER ADMIN                              │
├─────────────────────────────────────────────────────────────┤
│  TENANT 1 (Église A)     │  TENANT 2 (Église B)     │ ...   │
│  ┌─────────────────────┐ │ ┌─────────────────────┐  │       │
│  │ Pastor (Admin)      │ │ │ Pastor (Admin)      │  │       │
│  │ ├─ Member 1         │ │ │ ├─ Member 1         │  │       │
│  │ ├─ Member 2         │ │ │ ├─ Member 2         │  │       │
│  │ └─ ...              │ │ │ └─ ...              │  │       │
│  └─────────────────────┘ │ └─────────────────────┘  │       │
└─────────────────────────────────────────────────────────────┘
```

## Architecture

### Technology Stack

- **Java 21**
- **Spring Boot 3.5.5** - Main framework
- **Spring Security 6** - Security and authentication
- **JJWT 0.11.5** - JWT token generation and validation
- **gRPC 1.68.1** - High-performance inter-module RPC
- **Protobuf 4.29.0** - Efficient binary serialization
- **OpenAPI Generator 7.15.0** - Automatic API generation
- **WebClient (Spring WebFlux)** - Fallback HTTP communication
- **MySQL 8.0** - Relational database
- **JaCoCo** - Code coverage (100%)
- **Maven** - Dependency management

### Applied Architectural Principles

-  **Clean Architecture** - Strict layer separation (Domain, Business, IO, Web)
-  **SOLID Principles** - Maintainable and extensible code
-  **Domain-Driven Design** - Pure domain objects (Member, TemporaryToken, UserTokens)
-  **Event-Driven Architecture** - Domain events for async operations
-  **Ports & Adapters** - MemberRepository (port), MembersClient, EmailClient (adapters)
-  **API-First Design** - OpenAPI Specification → Automatic generation
-  **Delegate Pattern** - Controller/Delegate separation for business logic
-  **AOP (Aspect-Oriented Programming)** - Cross-cutting concerns (logging, security)
-  **Resilience Patterns** - gRPC primary, WebClient fallback for fault tolerance

### Project Structure

```
src/
├── main/
│   ├── java/com/ecclesiaflow/springsecurity/
│   │   ├── application/                    # Application Layer (Configuration & Cross-cutting)
│   │   │   ├── config/                    # Spring configurations
│   │   │   │   ├── SecurityConfiguration.java
│   │   │   │   ├── GrpcClientConfig.java  # gRPC channel management
│   │   │   │   ├── GrpcServerConfig.java  # gRPC server startup
│   │   │   │   ├── AsyncConfig.java       # Async event processing
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── WebClientConfig.java
│   │   │   ├── handlers/                  # Event handlers (Application layer)
│   │   │   │   └── PasswordEventHandler.java  # Password events → Email actions
│   │   │   └── logging/aspect/            # AOP Aspects (cross-cutting concerns)
│   │   │       ├── BusinessOperationLoggingAspect.java
│   │   │       ├── GrpcClientLoggingAspect.java
│   │   │       ├── GrpcServerLoggingAspect.java
│   │   │       ├── EmailEventLoggingAspect.java
│   │   │       └── LoggingAspect.java
│   │   │
│   │   ├── business/                       # Business Layer (Pure Business Logic)
│   │   │   ├── domain/                    # Domain objects + Ports (interfaces)
│   │   │   │   ├── email/                 # Email domain
│   │   │   │   │   └── EmailClient.java   # Port (interface)
│   │   │   │   ├── member/                # Member domain
│   │   │   │   │   ├── Member.java        # Domain entity
│   │   │   │   │   ├── Role.java
│   │   │   │   │   ├── MemberRepository.java   # Port (interface)
│   │   │   │   │   └── MembersClient.java      # Port (interface)
│   │   │   │   ├── password/              # Password domain
│   │   │   │   │   ├── SigninCredentials.java
│   │   │   │   │   └── PasswordManagement.java
│   │   │   │   ├── security/              # Security domain
│   │   │   │   │   └── Scope.java         # Permission enumeration
│   │   │   │   └── token/                 # Token domain
│   │   │   │       ├── UserTokens.java
│   │   │   │       ├── TemporaryToken.java
│   │   │   │       └── TokenCredentials.java
│   │   │   ├── encryption/                # PasswordEncoderUtil
│   │   │   ├── events/                    # Domain events (event-driven)
│   │   │   │   ├── PasswordSetEvent.java
│   │   │   │   ├── PasswordChangedEvent.java
│   │   │   │   ├── PasswordResetRequestedEvent.java
│   │   │   │   └── PasswordResetEvent.java
│   │   │   ├── exceptions/                # Business exceptions
│   │   │   └── services/                  # Business services (use cases)
│   │   │       ├── AuthenticationService.java
│   │   │       ├── PasswordService.java
│   │   │       ├── adapters/              # MemberUserDetailsAdapter
│   │   │       ├── impl/                  # Service implementations
│   │   │       └── mappers/               # Business mappers
│   │   │
│   │   ├── io/                            # IO Layer (Adapters - Hexagonal Architecture)
│   │   │   ├── email/                     # Email adapters (by domain)
│   │   │   │   └── EmailGrpcClient.java   # gRPC implementation of EmailClient port
│   │   │   ├── members/                   # Members adapters (by domain)
│   │   │   │   └── MembersGrpcClient.java # gRPC implementation of MembersClient port
│   │   │   ├── grpc/                      # gRPC server endpoints
│   │   │   │   └── server/
│   │   │   │       └── JwtGrpcServiceImpl.java  # Auth service exposed via gRPC
│   │   │   └── persistence/               # Persistence adapters
│   │   │       ├── jpa/                   # JPA entities
│   │   │       ├── mappers/               # Persistence mappers
│   │   │       └── repositories/impl/     # Repository implementations
│   │   │
│   │   └── web/                           # Web Layer (REST API)
│   │       ├── client/                    # MembersClientImpl (WebClient fallback)
│   │       ├── controller/                # AuthenticationController, PasswordController
│   │       ├── delegate/                  # AuthenticationDelegate, PasswordManagementDelegate
│   │       ├── exception/                 # InvalidCredentialsException, JwtProcessingException
│   │       ├── mappers/                   # OpenApiModelMapper, TemporaryTokenMapper, MemberMapper
│   │       └── security/                  # JwtProcessor, Jwt, CustomAuthenticationEntryPoint
│   └── resources/
│       ├── api/
│       │   └── openapi.yaml               # OpenAPI 3.1.1 Specification (API contract)
│       └── application.properties.example
│
└── test/                                   # Tests (100% coverage)
    └── java/com/ecclesiaflow/springsecurity/
        ├── application/                    # Configuration & AOP tests
        ├── business/                       # Business services tests
        ├── io/
        │   ├── email/                      # Email client tests
        │   ├── members/                    # Members client tests
        │   ├── grpc/                       # gRPC integration tests
        │   └── persistence/                # Persistence tests
        └── web/                           # Controllers/delegates tests
```

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- MySQL 8.0+
- Compatible IDE (IntelliJ IDEA recommended)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/your-org/ecclesiaflow-auth-module.git
cd ecclesiaflow-auth-module
```

2. **Configure the database**
```sql
CREATE DATABASE auth_module_db;
CREATE USER 'ecclesiaflow'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON auth_module_db.* TO 'ecclesiaflow'@'localhost';
```

3. **Configure environment variables**
```bash
# Copy the example file
cp .env.example .env

# Edit the variables
vim .env

# Copy the example application.properties file
cp src/main/resources/application.properties.example src/main/resources/application.properties

# Edit the variables (DB, JWT secret, etc.)
vim src/main/resources/application.properties
```

4. **Generate APIs from OpenAPI specification**
```bash
# The OpenAPI Generator plugin runs automatically during build
mvn clean generate-sources
```

5. **Mark generated sources as source root (IntelliJ IDEA)**
```
- Right-click on target/generated-sources/openapi/src/main/java
- Select "Mark Directory as" > "Generated Sources Root"

OR via Maven:
- The maven-build-helper-plugin automatically adds it as source directory
```

6. **Run the application**
```bash
mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081'
```

The application will be accessible at `http://localhost:8081`

### First Tests

```bash
# Check that the application is running
curl http://localhost:8081/actuator/health

# Access Swagger documentation
open http://localhost:8081/swagger-ui.html
```

## API Documentation

### Main Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/ecclesiaflow/auth/token` | POST | JWT token generation (access + refresh) | No |
| `/ecclesiaflow/auth/refreshToken` | POST | Access token refresh | Yes (Bearer) |
| `/ecclesiaflow/auth/temporary-token` | POST | Temporary token generation (confirmation) | No |
| `/ecclesiaflow/auth/password` | POST | Initial password setup (new member) | Yes (Bearer temp) |
| `/ecclesiaflow/auth/new-password` | POST | Password change (authenticated user) | Yes (Bearer) |
| `/ecclesiaflow/auth/forgot-password` | POST | Request password reset email | No |
| `/ecclesiaflow/auth/reset-password` | POST | Reset forgotten password | Yes (Bearer temp) |

**Available Scopes:**
- `ef:members:read:own` - Read own information
- `ef:members:write:own` - Modify own information
- `ef:members:delete:own` - Delete own account
- `ef:members:read:all` - Read all information (ADMIN)
- `ef:members:write:all` - Modify all information (ADMIN)
- `ef:members:delete:all` - Delete any account (ADMIN)

### Usage Examples

**1. Authentication (token generation):**
```bash
curl -X POST http://localhost:8081/ecclesiaflow/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "email": "membre@eglise.com",
    "password": "MotDePasse123!"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**2. Token refresh:**
```bash
curl -X POST http://localhost:8081/ecclesiaflow/auth/refreshToken \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_REFRESH_TOKEN" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

**3. Initial password setup (after email confirmation):**
```bash
curl -X POST http://localhost:8081/ecclesiaflow/auth/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TEMPORARY_TOKEN" \
  -d '{
    "password": "NewPassword123!"
  }'
```

**4. Request password reset (forgot password):**
```bash
curl -X POST http://localhost:8081/ecclesiaflow/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "membre@eglise.com"
  }'
```

**Response:**
```json
{
  "message": "Si un compte existe avec cet email, un lien de réinitialisation a été envoyé."
}
```

**5. Reset password with token:**
```bash
curl -X POST http://localhost:8081/ecclesiaflow/auth/reset-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer RESET_TOKEN_FROM_EMAIL" \
  -d '{
    "password": "NewSecurePassword456!"
  }'
```

**Response:**
```json
{
  "message": "Mot de passe réinitialisé avec succès",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900
}
```

**6. Change password (authenticated user):**
```bash
curl -X POST http://localhost:8081/ecclesiaflow/auth/new-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "currentPassword": "OldPassword123!",
    "newPassword": "NewPassword456!"
  }'
```

**Response:**
```json
{
  "message": "Mot de passe modifié avec succès",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900
}
```

### Interactive Documentation

- **Swagger UI**: `http://localhost:8081/swagger-ui/index.html`
- **OpenAPI Spec**: `http://localhost:8081/v3/api-docs`
- **Source YAML file**: `src/main/resources/api/openapi.yaml`

### OpenAPI Generator Architecture

```
openapi.yaml (source of truth)
    ↓ (mvn clean generate-sources)
OpenAPI Generator Plugin
    ↓ generates
target/generated-sources/openapi/
    ├── api/                    # Interfaces (AuthenticationApi, PasswordManagementApi)
    ├── model/                  # DTOs (SigninRequest, JwtAuthenticationResponse, etc.)
    └── ApiUtil.java
    ↓ implemented by
Controllers (AuthenticationController, PasswordController)
    ↓ delegate to
Delegates (AuthenticationDelegate, PasswordManagementDelegate)
    ↓ use
Business Services (AuthenticationService, PasswordService, Jwt)
```

### Password Change Flow When Already Connected (Auth Module)

```mermaid
sequenceDiagram
    participant Auth
    participant Email

    Note over Auth: 1. Change Password (REST)
    Auth->>Auth: POST /new-password<br/>(Bearer access token)
    Auth->>Auth: Extract email from JWT
    Auth->>Auth: Validate current password
    Auth->>Auth: Validate token issuedAt
    Auth->>Auth: Update password
    Auth->>Auth: Publish PasswordChangedEvent
    Auth->>Auth: Generate new tokens

    Note over Auth,Email: 2. Async Notification (Event)
    Auth->>Email: Send confirmation
```

**Key Points:**
- **Email from JWT**: Extracted automatically (security)
- **Token validation**: Issued after last password change
- **Async notification**: Email doesn't block operation
- **New tokens**: Force re-authentication with new credentials

### Password Reset Flow (Auth Module)

```mermaid
sequenceDiagram
    participant Auth
    participant Email

    Note over Auth: 1. Request Reset (REST)
    Auth->>Auth: POST /forgot-password {email}
    Auth->>Auth: Publish PasswordResetRequestedEvent
    Auth->>Auth: Return generic message

    Note over Auth,Email: 2. Async Email (Event)
    Auth->>Auth: Generate JWT (purpose: reset)
    Auth->>Email: Send reset link

    Note over Auth: 3. Reset Password (REST)
    Auth->>Auth: POST /reset-password (Bearer token)
    Auth->>Auth: Validate token + purpose
    Auth->>Auth: Update password
    Auth->>Auth: Publish PasswordResetEvent
    Auth->>Auth: Generate tokens

    Note over Auth,Email: 4. Async Confirmation (Event)
    Auth->>Email: Send confirmation
```

**Key Points:**
- **Security**: Generic message prevents email enumeration
- **Async emails**: Non-blocking operations
- **Event-driven**: Resilient architecture
- **Auto-login**: Returns tokens after reset

## Configuration

### Spring Profiles

- **`dev`** - Local development with H2
- **`test`** - Automated tests
- **`prod`** - Production with MySQL

```bash
# Run with a specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Security

### Security Features

- **JWT Tokens** - Stateless authentication
- **Refresh Tokens** - Automatic renewal
- **Input Validation** - Strict input data validation
- **Password Encoding** - Secure BCrypt hashing
- **Audit Logging** - Traceability of critical operations

### Security Configuration

```java
// JWT configuration example
@Value("${jwt.expiration:900000}")
private Long jwtExpiration; // 15 minutes

```

### Applied Best Practices

-  **Externalized secrets** - No hardcoded secrets in code
-  **Short-lived tokens** - Access token: 15 minutes, Refresh: 7d, Temp: 15min
-  **Refresh tokens** - Automatic renewal for better UX
-  **Strict validation** - Bean Validation (Jakarta) on all DTOs
-  **Audit logging** - Authentication attempt traceability via AOP
-  **Granular scopes** - Fine-grained permissions (own/all) for each resource
-  **Password encoding** - BCrypt with automatic salt

## Tests

### Test Statistics

- **509 tests** passing successfully 
- **38 test files**
- **Code coverage: 100%** (JaCoCo)
- **Branch coverage: 100%**
- **Production code: 5,148 lines**
- **Test code: 11,573 lines**
- **No ignored or disabled tests**

### Running Tests

```bash
# All tests
mvn test

# Tests with coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html

# Tests for a specific package
mvn test -Dtest="com.ecclesiaflow.springsecurity.web.security.*"
```

### Test Structure

```
src/test/java/com/ecclesiaflow/springsecurity/
├── application
│   ├── config
│   └── logging
├── business
│   ├── domain
│   ├── encryption
│   └── services
├── io
│   └── persistence
└── web
    ├── client
    ├── controller
    ├── delegate
    ├── exception
    ├── mappers
    └── security
```

## Deployment

### Production Build

```bash
# Create JAR with all dependencies
mvn clean package

# JAR will be in target/
ls target/ecclesiaflow-auth-module-*.jar

# Verify JAR contains generated APIs
jar tf target/ecclesiaflow-auth-module-*.jar | grep -i openapi

# Run the JAR
java -jar target/ecclesiaflow-auth-module-*.jar --spring.profiles.active=prod
```

### Configuration for Production

**Important production settings:**

```properties
# application-prod.properties

# Enable gRPC for inter-module communication
grpc.enabled=true
grpc.server.port=9090
grpc.client.shutdown-timeout-seconds=5

# Database connection pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# JWT security
jwt.expiration=900000                    # 15 minutes
jwt.refresh-token-expiration=604800000   # 7 days

# Logging
logging.level.com.ecclesiaflow=INFO
logging.level.io.grpc=WARN
```

## Contribution

### Development Workflow

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Code Standards

-  **Clean Architecture** - Respect layer separation
-  **Mandatory tests** - Minimum 90% coverage
-  **OpenAPI First** - Modify `openapi.yaml` before code
-  **Atomic commits** - Conventional commit messages
-  **Documentation** - Javadoc for public classes
-  **Code review** - At least 1 approval required

### **Commit Convention**

**Format with type:**
```
Type(scope): description (≤ 50 characters, first letter capitalized)

Message body (≤ 72 characters per line)

Types: Feat, Fix, Docs, Style, Refactor, Test, Chore
Scopes: members, confirmation, email, persistence, web

NB: scope is optional
```

**Format without type:**
```
Add new feature (≤ 50 characters, first letter capitalized)

Detailed message body if necessary
(≤ 72 characters per line)
```

**Examples:**
- `Feat(members): add email validation service`
- `Fix(confirmation): resolve code expiration issue`
- `Feat: resolve code expiration issue`
- `Add comprehensive member profile validation`
- `Update OpenAPI documentation for new endpoints`

---

### Pull Request Checklist

- [ ] Tests pass (`mvn test`)
- [ ] Coverage is ≥ 90% (`mvn jacoco:report`)
- [ ] Code compiles without warnings (`mvn clean compile`)
- [ ] Documentation is up to date (README, Javadoc)
- [ ] Commits follow Conventional Commits
- [ ] `openapi.yaml` file is valid
- [ ] Generated APIs compile correctly

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Useful Links

### Official Documentation

- [Spring Boot 3.5.5](https://spring.io/projects/spring-boot)
- [Spring Security 6](https://spring.io/projects/spring-security)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [OpenAPI Specification 3.1](https://swagger.io/specification/)
- [JJWT Library](https://github.com/jwtk/jjwt)
- [JaCoCo Code Coverage](https://www.jacoco.org/jacoco/)

### Development Tools

- [JWT.io](https://jwt.io/) - JWT decoder and validator
- [Swagger Editor](https://editor.swagger.io/) - Online OpenAPI editor
- [Postman](https://www.postman.com/) - REST API client

### Architecture and Patterns

- [Clean Architecture (Uncle Bob)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Delegate Pattern](https://refactoring.guru/design-patterns/proxy)
- [gRPC Documentation](https://grpc.io/docs/)
- [Protocol Buffers](https://protobuf.dev/)

---

**Developed with ❤️ for the EcclesiaFlow community**

*Centralized Authentication Module - Version 1.0.0-SNAPSHOT*