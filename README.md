# EcclesiaFlow Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.9-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)]()

> **Modern SaaS church management platform with microservices architecture**

A comprehensive platform that enables churches to efficiently manage their community, events, and resources. Multi-tenant, secure, and intuitive, it provides tools for administration, communication, and member engagement.

---

## 🚀 Overview

EcclesiaFlow is built as a distributed microservices platform where each church operates as an independent tenant with its own administrator (pastor) and members.  
The platform supports **scalable, secure operations** across multiple churches while maintaining **data isolation** and customization capabilities.

```mermaid

sequenceDiagram
    participant Client
    participant Members as Members Module (8080)
    participant Auth as Auth Module (8081)

    Client->>Members: POST /members (create member)
    Members-->>Client: Confirmation code sent

    Client->>Members: POST /members/{id}/confirmation (with code)
    Members-->>Client: Member confirmed

    Members->>Auth: Request temporary token
    Auth-->>Members: Temporary token

    Members-->>Client: Temporary token

    Client->>Auth: POST /set-password (with temp token + new password)
    Auth-->>Client: Password set successfully


````

---

## ✨ Core Features

* 🔐 **Secure Authentication** – Centralized JWT-based authentication with role management
* 👥 **Member Management** – Complete member lifecycle from registration to confirmation
* 📅 **Event Organization** – Service planning and event management tools
* 💰 **Financial Management** – Donation tracking and financial reporting
* 🌐 **Multi-language Support** – Internationalization ready
* 📱 **Mobile & Web Applications** – Responsive design across platforms
* 📊 **Analytics & Reporting** – Comprehensive dashboards and insights

---

## 📦 Modules & Source Code

* **Authentication Module**
  🔗 [GitHub Repo](https://github.com/GYOM15/ecclesiaflow-auth-module)
  **Port**: 8081
  **Purpose**: Centralized authentication and authorization service

* **Members Module**
  🔗 [GitHub Repo](https://github.com/GYOM15/ecclesiaflow-members-module)
  **Port**: 8080
  **Purpose**: Member management and profile services

* **Events Module** *(planned)* – Event planning and service management

* **Finance Module** *(planned)* – Donation tracking and financial reporting

---

## 🛠 Technology Stack

* **Backend**: Java 21, Spring Boot 3.4.9
* **Security**: Spring Security 6, JWT
* **Database**: MySQL 8.0+
* **Communication**: Spring WebFlux, WebClient
* **Documentation**: OpenAPI 3.0, Swagger UI
* **Build Tool**: Maven 3.8+
* **Architecture**: Microservices, Clean Architecture

---

## ⚡ Quick Start

### 1. Prerequisites

* Java 21+
* Maven 3.8+
* MySQL 8.0+
* IDE (IntelliJ IDEA recommended)

### 2. Clone

```bash
git clone https://github.com/ecclesiaflow/ecclesiaflow-platform.git
cd ecclesiaflow-platform
```

### 3. Database Setup

```sql
-- Auth Module Database
CREATE DATABASE ecclesiaflow_auth;
CREATE USER 'ecclesiaflow'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON ecclesiaflow_auth.* TO 'ecclesiaflow'@'localhost';

-- Members Module Database
CREATE DATABASE ecclesiaflow_members;
GRANT ALL PRIVILEGES ON ecclesiaflow_members.* TO 'ecclesiaflow'@'localhost';
```

### 4. Start Modules

```bash
# Auth Module
cd auth-module
mvn spring-boot:run -Dserver.port=8081

# Members Module (in another terminal)
cd members-module
mvn spring-boot:run -Dserver.port=8080
```

### 5. Test Health Endpoints

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8080/actuator/health
```

---

## 🔑 API Examples (cURL + jq)

```bash
# Create a member
curl -X POST "http://localhost:8080/ecclesiaflow/members" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Caleb","lastName":"Tolno","email":"caleb@example.com","address":"123 Test St"}' | jq .

# Authenticate & Get JWT (Auth module)
curl -X POST "http://localhost:8081/ecclesiaflow/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"email":"caleb@example.com","password":"Pass123!"}' | jq .

# Get Members List (Authenticated)
curl -X GET "http://localhost:8080/ecclesiaflow/members" \
  -H "Authorization: Bearer <access_token>" | jq .

# Refresh Token
curl -X POST "http://localhost:8081/ecclesiaflow/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token>"}' | jq .
```

---

## ⚙️ Configuration

**Common Variables**

```bash
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=ecclesiaflow
DB_PASSWORD=your_password
JWT_SECRET=your_256_bit_secret
JWT_TOKEN_EXPIRATION=86400000
JWT_REFRESH_TOKEN_EXPIRATION=604800000
```

**Auth Module (.env)**

```bash
DB_NAME=ecclesiaflow_auth
SERVER_PORT=8081
```

**Members Module (.env)**

```bash
DB_NAME=ecclesiaflow_members
SERVER_PORT=8080
AUTH_MODULE_BASE_URL=http://localhost:8081
```

---

## 📦 Deployment (Docker Compose)

```yaml
version: '3.8'
services:
  auth-module:
    build: ./auth-module
    ports:
      - "8081:8081"
    environment:
      - DB_NAME=ecclesiaflow_auth
    depends_on:
      - mysql

  members-module:
    build: ./members-module
    ports:
      - "8080:8080"
    environment:
      - DB_NAME=ecclesiaflow_members
      - AUTH_MODULE_BASE_URL=http://auth-module:8081
    depends_on:
      - mysql
      - auth-module

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

---

## 🤝 Contributing

**Branch Strategy**

* `main` – documentation
* `auth-module` – auth service dev
* `members-module` – members service dev
* `events-module` – planned
* `finance-module` – planned

**Workflow**

1. Checkout relevant module
2. Create feature branch
3. Develop & test
4. Open PR to module branch
5. Merge after review
6. Integration testing across modules

---

## 📄 License

MIT – see [LICENSE](LICENSE)

---

**Developed with ❤️ for church communities worldwide**


