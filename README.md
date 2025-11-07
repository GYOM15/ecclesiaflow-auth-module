# 🔐 EcclesiaFlow Authentication Module

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-509%20passing-success.svg)]()
[![Coverage](https://img.shields.io/badge/Coverage-100%25-brightgreen.svg)]()

> **Centralized authentication module for the EcclesiaFlow church management platform**

A robust and secure authentication service designed to support EcclesiaFlow's multi-tenant architecture, where each church constitutes an independent tenant with its own administrator (pastor) and members. Automatic API generation via OpenAPI Generator with Delegate pattern.

## 📋 Table of Contents

- [🎯 Overview](#-overview)
- [🏗️ Architecture](#️-architecture)
- [🚀 Quick Start](#-quick-start)
- [📚 API Documentation](#-api-documentation)
- [🔧 Configuration](#-configuration)
- [🛡️ Security](#️-security)
- [🧪 Tests](#-tests)
- [📦 Deployment](#-deployment)
- [🤝 Contribution](#-contribution)

## 🎯 Overview

### Module Objective

This module provides centralized authentication services for the EcclesiaFlow ecosystem:

- **JWT token generation** (access + refresh) for resource access
- **Automatic refresh** of expired tokens
- **Password management** (initial setup + change)
- **Temporary tokens** for registration confirmation
- **Granular scope system** for permissions
- **Multi-tenant support** ready for distributed architecture
- **API-First Design** with automatic generation via OpenAPI Generator
- **gRPC bi-directional communication** with Members module (Auth ↔ Members)
- **Resilient inter-module communication** with WebClient fallback

### Target Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SUPER ADMIN                              │
├─────────────────────────────────────────────────────────────┤
│  TENANT 1 (Église A)    │  TENANT 2 (Église B)    │ ...    │
│  ┌─────────────────────┐ │ ┌─────────────────────┐  │        │
│  │ Pastor (Admin)      │ │ │ Pastor (Admin)      │  │        │
│  │ ├─ Member 1         │ │ │ ├─ Member 1         │  │        │
│  │ ├─ Member 2         │ │ │ ├─ Member 2         │  │        │
│  │ └─ ...              │ │ │ └─ ...              │  │        │
│  └─────────────────────┘ │ └─────────────────────┘  │        │
└─────────────────────────────────────────────────────────────┘
```

## 🏗️ Architecture

### Technology Stack

- **Java 21** - LTS with latest features (Records, Pattern Matching)
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

- ✅ **Clean Architecture** - Strict layer separation (Domain, Business, IO, Web)
- ✅ **SOLID Principles** - Maintainable and extensible code
- ✅ **Domain-Driven Design** - Pure domain objects (Member, TemporaryToken, UserTokens)
- ✅ **Ports & Adapters** - MemberRepository (port), MembersClient (adapter)
- ✅ **API-First Design** - OpenAPI Specification → Automatic generation
- ✅ **Delegate Pattern** - Controller/Delegate separation for business logic
- ✅ **AOP (Aspect-Oriented Programming)** - Cross-cutting concerns (logging, security)
- ✅ **Resilience Patterns** - gRPC primary, WebClient fallback for fault tolerance

### Project Structure (Clean Architecture)

```
src/
├── main/
│   ├── java/com/ecclesiaflow/springsecurity/
│   │   ├── application/                    # Application Layer
│   │   │   └── logging/aspect/            # AOP Aspects (AuthenticationErrorLoggingAspect)
│   │   ├── business/                       # Business Layer (Business Logic)
│   │   │   ├── domain/                    # Pure domain objects
│   │   │   │   ├── member/                # Member, Role, MemberRepository (port)
│   │   │   │   ├── password/              # SigninCredentials, PasswordManagement
│   │   │   │   ├── security/              # Scope (permission enumeration)
│   │   │   │   └── token/                 # UserTokens, TemporaryToken, TokenCredentials
│   │   │   ├── encryption/                # PasswordEncoderUtil
│   │   │   ├── exceptions/                # MemberNotFoundException
│   │   │   └── services/                  # Business services
│   │   │       ├── AuthenticationService
│   │   │       ├── PasswordService
│   │   │       ├── adapters/              # MemberUserDetailsAdapter
│   │   │       ├── impl/                  # Implementations
│   │   │       └── mappers/               # ScopeMapper
│   │   ├── io/                            # IO Layer (External Communication)
│   │   │   ├── grpc/                      # gRPC Communication
│   │   │   │   ├── client/                # MembersGrpcClient (Auth → Members)
│   │   │   │   └── server/                # JwtGrpcServiceImpl (Members → Auth)
│   │   │   └── persistence/
│   │   │       ├── jpa/                   # MemberEntity, SpringDataMemberRepository
│   │   │       ├── mappers/               # MemberPersistenceMapper
│   │   │       └── repositories/          # MemberRepositoryImpl
│   │   └── web/                           # Web Layer (REST API)
│   │       ├── client/                    # MembersClientImpl (WebClient fallback)
│   │       ├── controller/                # AuthenticationController, PasswordController
│   │       ├── delegate/                  # AuthenticationDelegate, PasswordManagementDelegate
│   │       ├── exception/                 # InvalidCredentialsException, JwtProcessingException
│   │       ├── mappers/                   # OpenApiModelMapper, TemporaryTokenMapper, MemberMapper
│   │       └── security/                  # JwtProcessor, Jwt, CustomAuthenticationEntryPoint
│   └── resources/
│       ├── api/
│       │   └── openapi.yaml               # OpenAPI 3.1.1 Specification
│       └── application.properties.example
└── test/                                   # 509 tests (100% coverage)
    └── java/com/ecclesiaflow/springsecurity/
        ├── application/                    # AOP aspects tests
        ├── business/                       # Business services tests
        ├── io/
        │   ├── grpc/                       # gRPC integration tests
        │   └── persistence/                # Persistence tests
        └── web/                           # Controllers/delegates tests
```

## 🚀 Quick Start

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
CREATE DATABASE ecclesiaflow_auth;
CREATE USER 'ecclesiaflow'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON ecclesiaflow_auth.* TO 'ecclesiaflow'@'localhost';
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
mvn spring-boot:run
```

The application will be accessible at `http://localhost:8081`

### First Tests

```bash
# Check that the application is running
curl http://localhost:8080/actuator/health

# Access Swagger documentation
open http://localhost:8080/swagger-ui.html
```

## 📚 API Documentation

### Main Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/ecclesiaflow/auth/token` | POST | JWT token generation (access + refresh) | No |
| `/ecclesiaflow/auth/refreshToken` | POST | Access token refresh | Yes (Bearer) |
| `/ecclesiaflow/auth/temporary-token` | POST | Temporary token generation (confirmation) | No |
| `/ecclesiaflow/auth/password` | POST | Initial password setup | Yes (Bearer temp) |
| `/ecclesiaflow/auth/new-password` | POST | Password change | Yes (Bearer) |

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
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "memberId": "550e8400-e29b-41d4-a716-446655440000",
  "scopes": ["ef:members:read:own", "ef:members:write:own"]
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

### Inter-Module Communication Architecture

The authentication module communicates with the Members module using a **resilient dual-protocol approach**:

```
┌─────────────────────┐         gRPC (Primary)         ┌─────────────────────┐
│   Auth Module       │◄─────────────────────────────► │  Members Module     │
│   (Port 8081)       │                                │   (Port 8080)       │
│                     │                                │                     │
│  gRPC Server: 9090  │                                │  gRPC Server: 9091  │
│  gRPC Client ───────┼────────► gRPC Server           │                     │
│                     │                                │                     │
│  WebClient ─────────┼────────► REST API              │                     │
│  (Fallback)         │   WebClient (Fallback)         │                     │
└─────────────────────┘                                └─────────────────────┘
```

**gRPC Communication Flows:**

1. **Auth → Members (gRPC Client)**
   - Check email confirmation status
   - Primary method: high-performance binary protocol
   - Fallback: WebClient HTTP/REST if gRPC unavailable

2. **Members → Auth (gRPC Server)**
   - Generate temporary JWT tokens for email confirmation
   - Members calls Auth's gRPC service for token generation
   - Tokens used for password setup after email confirmation

**Resilience Strategy:**
- gRPC enabled by default (`grpc.enabled=true`)
- Automatic fallback to WebClient if gRPC server unavailable
- Graceful degradation ensures service continuity
- Separate ports: REST (8080/8081), gRPC (9090/9091)

## 🔧 Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=ecclesiaflow_auth
DB_USERNAME=ecclesiaflow
DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your-super-secret-key-minimum-512-bits-base64
JWT_EXPIRATION=900000           # Access token: 15 minutes
JWT_REFRESH_TOKEN_EXPIRATION=604800000  # Refresh token: 7 days
JWT_TEMPORARY_TOKEN_EXPIRATION=900000   # Temporary token: 15 minutes

# Inter-Module Communication
ECCLESIAFLOW_AUTH_MODULE_BASE_URL=http://localhost:8081
ECCLESIAFLOW_MEMBERS_MODULE_BASE_URL=http://localhost:8080

# gRPC Configuration
GRPC_ENABLED=true
GRPC_SERVER_PORT=9090
GRPC_AUTH_HOST=localhost
GRPC_AUTH_PORT=9090
GRPC_MEMBERS_HOST=localhost
GRPC_MEMBERS_PORT=9091

# Default Admin (development only)
ADMIN_EMAIL=admin@ecclesiaflow.com
ADMIN_PASSWORD=Admin123!
ADMIN_FIRST_NAME=Admin
ADMIN_LAST_NAME=EcclesiaFlow

# Email Configuration (Gmail SMTP)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@ecclesiaflow.com
```

### Spring Profiles

- **`dev`** - Local development with H2
- **`test`** - Automated tests
- **`prod`** - Production with MySQL

```bash
# Run with a specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 🛡️ Security

### Security Features

- **🔐 JWT Tokens** - Stateless authentication
- **🔄 Refresh Tokens** - Automatic renewal
- **🛡️ Rate Limiting** - Protection against brute force attacks
- **✅ Input Validation** - Strict input data validation
- **🔒 Password Encoding** - Secure BCrypt hashing
- **📝 Audit Logging** - Traceability of critical operations

### Security Configuration

```java
// JWT configuration example
@Value("${jwt.expiration:900000}")
private Long jwtExpiration; // 15 minutes

```

### Applied Best Practices

- ✅ **Externalized secrets** - No hardcoded secrets in code
- ✅ **Short-lived tokens** - Access token: 15 minutes, Refresh: 7d, Temp: 15min
- ✅ **Refresh tokens** - Automatic renewal for better UX
- ✅ **Strict validation** - Bean Validation (Jakarta) on all DTOs
- ✅ **Audit logging** - Authentication attempt traceability via AOP
- ✅ **Granular scopes** - Fine-grained permissions (own/all) for each resource
- ✅ **Password encoding** - BCrypt with automatic salt

## 🧪 Tests

### Test Statistics

- **509 tests** passing successfully ✅
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

## 📦 Deployment

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

## 🤝 Contribution

### Development Workflow

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Code Standards

- ✅ **Clean Architecture** - Respect layer separation
- ✅ **Mandatory tests** - Minimum 90% coverage
- ✅ **OpenAPI First** - Modify `openapi.yaml` before code
- ✅ **Atomic commits** - Conventional commit messages
- ✅ **Documentation** - Javadoc for public classes
- ✅ **Code review** - At least 1 approval required

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

## 🔗 Useful Links

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

## 📊 Project Metrics

| Metric              | Value      |
|---------------------|------------|
| **Production code** | 5,148 LOC  |
| **Test code**       | 11,573 LOC |
| **Tests**           | 509        |
| **Test files**      | 38         |
| **Coverage**        | 100%       |
| **Classes**         | 89         |
| **Endpoints**       | 9          |
| **Scopes**          | 8          |
| **Dependencies**    | 25         |

---

**Developed with ❤️ for the EcclesiaFlow community**

*Centralized Authentication Module - Version 1.0.0-SNAPSHOT*