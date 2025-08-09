# 🔐 EcclesiaFlow Authentication Module

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)]()

> **Module d'authentification centralisée pour la plateforme de gestion d'église EcclesiaFlow**

Un service d'authentification robuste et sécurisé conçu pour supporter l'architecture multi-tenant d'EcclesiaFlow, où chaque église constitue un tenant indépendant avec son propre administrateur (pasteur) et ses membres.

## 📋 Table des matières

- [🎯 Vue d'ensemble](#-vue-densemble)
- [🏗️ Architecture](#️-architecture)
- [🚀 Démarrage rapide](#-démarrage-rapide)
- [📚 API Documentation](#-api-documentation)
- [🔧 Configuration](#-configuration)
- [🛡️ Sécurité](#️-sécurité)
- [🧪 Tests](#-tests)
- [📦 Déploiement](#-déploiement)
- [🤝 Contribution](#-contribution)

## 🎯 Vue d'ensemble

### Objectif du module

Ce module fournit les services d'authentification centralisée pour l'écosystème EcclesiaFlow :

- **Génération de tokens JWT** pour l'accès aux ressources
- **Rafraîchissement automatique** des tokens expirés
- **Validation des identifiants** avec sécurité renforcée
- **Support multi-tenant** prêt pour l'architecture distribuée

### Architecture cible

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

### Stack technologique

- **Java 21** - LTS avec les dernières fonctionnalités
- **Spring Boot 3.2** - Framework principal
- **Spring Security 6** - Sécurité et authentification
- **JWT** - Tokens stateless pour la scalabilité
- **MySQL** - Base de données relationnelle
- **Maven** - Gestion des dépendances

### Principes architecturaux appliqués

- ✅ **Clean Architecture** - Séparation claire des couches
- ✅ **SOLID Principles** - Code maintenable et extensible
- ✅ **Domain-Driven Design** - Logique métier centralisée
- ✅ **AOP (Aspect-Oriented Programming)** - Logging transversal
- ✅ **API-First Design** - Documentation OpenAPI complète

### Structure du projet

```
src/
├── main/
│   ├── java/com/ecclesiaflow/springsecurity/
│   │   ├── annotation/          # Annotations personnalisées
│   │   ├── aspect/              # Aspects AOP (logging)
│   │   ├── config/              # Configuration Spring
│   │   ├── controller/          # Contrôleurs REST
│   │   ├── domain/              # Objets métier
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── entities/            # Entités JPA
│   │   ├── exception/           # Gestion des exceptions
│   │   ├── services/            # Services métier
│   │   └── util/                # Utilitaires et mappers
│   └── resources/
│       ├── api/                 # Spécifications OpenAPI
│       └── application.properties
└── test/                        # Tests unitaires et d'intégration
```

## 🚀 Démarrage rapide

### Prérequis

- Java 21 ou supérieur
- Maven 3.8+
- MySQL 8.0+
- IDE compatible (IntelliJ IDEA recommandé)

### Installation

1. **Cloner le repository**
```bash
git clone https://github.com/your-org/ecclesiaflow-auth-module.git
cd ecclesiaflow-auth-module
```

2. **Configurer la base de données**
```sql
CREATE DATABASE ecclesiaflow_auth;
CREATE USER 'ecclesiaflow'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON ecclesiaflow_auth.* TO 'ecclesiaflow'@'localhost';
```

3. **Configurer les variables d'environnement**
```bash
# Copier le fichier d'exemple
cp .env.example .env

# Éditer les variables
vim .env
```

4. **Lancer l'application**
```bash
mvn spring-boot:run
```

L'application sera accessible sur `http://localhost:8080`

### Premiers tests

```bash
# Vérifier que l'application fonctionne
curl http://localhost:8080/actuator/health

# Accéder à la documentation Swagger
open http://localhost:8080/swagger-ui.html
```

## 📚 API Documentation

### Endpoints principaux

| Endpoint | Méthode | Description | Auth requise |
|----------|---------|-------------|--------------|
| `/ecclesiaflow/auth/token` | POST | Génération de token JWT | Non |
| `/ecclesiaflow/auth/refresh` | POST | Rafraîchissement de token | Non |
| `/ecclesiaflow/members/hello` | GET | Test d'authentification | Oui |
| `/ecclesiaflow/members/signup` | POST | Inscription temporaire* | Non |

*\*Endpoint temporaire - sera migré vers le module de gestion des membres*

### Exemples d'utilisation

**Authentification :**
```bash
curl -X POST http://localhost:8080/ecclesiaflow/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "email": "membre@eglise.com",
    "password": "motdepasse123"
  }'
```

**Utilisation du token :**
```bash
curl -X GET http://localhost:8080/ecclesiaflow/members/hello \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Documentation interactive

- **Swagger UI** : `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI Spec** : `http://localhost:8080/v3/api-docs`
- **Fichier YAML** : `src/main/resources/api/openapi.yaml`

## 🔧 Configuration

### Variables d'environnement

```bash
# Base de données
DB_HOST=localhost
DB_PORT=3306
DB_NAME=ecclesiaflow_auth
DB_USERNAME=ecclesiaflow
DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your-super-secret-key-minimum-256-bits
JWT_EXPIRATION=86400000  # 24 heures en millisecondes

# Admin par défaut
ADMIN_EMAIL=admin@ecclesiaflow.com
ADMIN_PASSWORD=admin123
```

### Profils Spring

- **`dev`** - Développement local avec H2
- **`test`** - Tests automatisés
- **`prod`** - Production avec MySQL

```bash
# Lancer avec un profil spécifique
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 🛡️ Sécurité

### Fonctionnalités de sécurité

- **🔐 JWT Tokens** - Authentification stateless
- **🔄 Refresh Tokens** - Renouvellement automatique
- **🛡️ Rate Limiting** - Protection contre les attaques par force brute
- **✅ Input Validation** - Validation stricte des données d'entrée
- **🔒 Password Encoding** - Hachage BCrypt sécurisé
- **📝 Audit Logging** - Traçabilité des opérations critiques

### Configuration de sécurité

```java
// Exemple de configuration JWT
@Value("${jwt.expiration:86400000}")
private Long jwtExpiration; // 24 heures

// Rate limiting sur les endpoints sensibles
@RateLimited(requests = 5, window = "1m")
public ResponseEntity<JwtAuthenticationResponse> generateToken(...)
```

### Bonnes pratiques appliquées

- ✅ Secrets externalisés (pas de hardcoding)
- ✅ Tokens avec expiration courte
- ✅ Refresh tokens pour l'UX
- ✅ Validation stricte des entrées
- ✅ Logging des tentatives d'authentification

## 🧪 Tests

### Lancer les tests

```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Tests avec couverture
mvn clean test jacoco:report
```

### Structure des tests

```
src/test/java/
├── unit/                    # Tests unitaires
│   ├── services/
│   └── controllers/
├── integration/             # Tests d'intégration
│   ├── api/
│   └── security/
└── fixtures/                # Données de test
```

## 📦 Déploiement

### Build de production

```bash
# Créer le JAR
mvn clean package -Pprod

# Le JAR sera dans target/
ls target/springsecurity-*.jar
```

### Docker

```dockerfile
FROM openjdk:21-jre-slim
COPY target/springsecurity-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build et run
docker build -t ecclesiaflow-auth .
docker run -p 8080:8080 ecclesiaflow-auth
```

## 🤝 Contribution

### Workflow de développement

1. **Fork** le repository
2. **Créer** une branche feature (`git checkout -b feature/amazing-feature`)
3. **Commit** vos changements (`git commit -m 'Add amazing feature'`)
4. **Push** vers la branche (`git push origin feature/amazing-feature`)
5. **Ouvrir** une Pull Request

### Standards de code

- **Commits atomiques** avec messages conventionnels
- **Tests** pour toute nouvelle fonctionnalité
- **Documentation** mise à jour
- **Code review** obligatoire

### Messages de commit

```
feat(auth): add multi-tenant support
fix(security): resolve JWT expiration issue
docs(api): update OpenAPI specification
refactor(services): improve password encoding
```

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 🔗 Liens utiles

- [Documentation Spring Boot](https://spring.io/projects/spring-boot)
- [Guide Spring Security](https://spring.io/guides/gs/securing-web/)
- [JWT.io](https://jwt.io/) - Décodeur JWT
- [OpenAPI Specification](https://swagger.io/specification/)

---

**Développé avec ❤️ pour la communauté EcclesiaFlow**