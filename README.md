# 🔐 EcclesiaFlow Authentication Module

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-428%20passing-success.svg)]()
[![Coverage](https://img.shields.io/badge/Coverage-90%25+-brightgreen.svg)]()

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
- **OpenAPI Generator 7.15.0** - Automatic API generation
- **MySQL 8.0** - Relational database
- **JaCoCo** - Code coverage (90%+)
- **Maven** - Dependency management

### Applied Architectural Principles

- ✅ **Clean Architecture** - Strict layer separation (Domain, Business, IO, Web)
- ✅ **SOLID Principles** - Maintainable and extensible code
- ✅ **Domain-Driven Design** - Pure domain objects (Member, TemporaryToken, UserTokens)
- ✅ **Hexagonal Architecture** - Ports & Adapters (MemberRepository, MembersClient)
- ✅ **API-First Design** - OpenAPI Specification → Automatic generation
- ✅ **Delegate Pattern** - Controller/Delegate separation for business logic
- ✅ **AOP (Aspect-Oriented Programming)** - Cross-cutting authentication error logging

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
│   │   ├── io/                            # IO Layer (Persistence)
│   │   │   └── persistence/
│   │   │       ├── jpa/                   # MemberEntity, SpringDataMemberRepository
│   │   │       ├── mappers/               # MemberPersistenceMapper
│   │   │       └── repositories/          # MemberRepositoryImpl
│   │   └── web/                           # Web Layer (REST API)
│   │       ├── client/                    # WebClient to communicate with members module
│   │       ├── controller/                # AuthenticationController, PasswordController
│   │       ├── delegate/                  # AuthenticationDelegate, PasswordManagementDelegate
│   │       ├── exception/                 # InvalidCredentialsException, JwtProcessingException
│   │       ├── mappers/                   # OpenApiModelMapper, TemporaryTokenMapper, MemberMapper
│   │       └── security/                  # JwtProcessor, Jwt, CustomAuthenticationEntryPoint
│   └── resources/
│       ├── api/
│       │   └── openapi.yaml               # OpenAPI 3.1.1 Specification
│       └── application.properties.example
└── test/                                   # 428 tests (90%+ coverage)
    └── java/com/ecclesiaflow/springsecurity/
        ├── application/                    # AOP aspects tests
        ├── business/                       # Business services tests
        ├── io/                            # Persistence tests
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

# Module Members Integration
ECCLESIAFLOW_AUTH_MODULE_BASE_URL=http://localhost:8081
ECCLESIAFLOW_MEMBERS_MODULE_BASE_URL=http://localhost:8080

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

- **428 tests** passing successfully ✅
- **Code coverage: 90%+** (JaCoCo)
- **Branch coverage: 90%+**
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
mvn clean package -DskipTests

# JAR will be in target/
ls target/ecclesiaflow-auth-module-*.jar

# Verify JAR
jar tf target/ecclesiaflow-auth-module-*.jar | grep -i openapi
```

### Docker

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/ecclesiaflow-auth-module-*.jar app.jar

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8081

EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
```

```bash
# Build and run
docker build -t ecclesiaflow-auth:latest .
docker run -p 8081:8081 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=ecclesiaflow_auth \
  -e DB_USERNAME=ecclesiaflow \
  -e DB_PASSWORD=your_password \
  -e JWT_SECRET=your-secret-key \
  ecclesiaflow-auth:latest
```

### Docker Compose

```yaml
version: '3.8'

services:
  auth-module:
    build: .
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=ecclesiaflow_auth
      - DB_USERNAME=ecclesiaflow
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - mysql
    networks:
      - ecclesiaflow-network

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=ecclesiaflow_auth
      - MYSQL_USER=ecclesiaflow
      - MYSQL_PASSWORD=${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - ecclesiaflow-network

volumes:
  mysql-data:

networks:
  ecclesiaflow-network:
    driver: bridge
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
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Delegate Pattern](https://refactoring.guru/design-patterns/proxy)

---

## 📊 Project Metrics

| Metric              | Value   |
|---------------------|---------|
| **Lines of code**   | ~15,000 |
| **Tests**           | 428     |
| **Coverage**        | 90%+    |
| **Classes**         | 80+     |
| **Endpoints**       | 5       |
| **Scopes**          | 8       |
| **Dependencies**    | 25      |

---

**Developed with ❤️ for the EcclesiaFlow community**

*Centralized Authentication Module - Version 1.0.0-SNAPSHOT*