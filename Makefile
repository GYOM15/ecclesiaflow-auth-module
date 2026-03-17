# =============================================================================
# EcclesiaFlow Auth Module — Makefile
# =============================================================================
# Usage:  make <target>
#         make help        — list all available targets
# =============================================================================

.DEFAULT_GOAL := help
SHELL         := /bin/bash

# ---------------------------------------------------------------------------
# Build & Test
# ---------------------------------------------------------------------------

.PHONY: build
build: ## Compile and generate OpenAPI + Protobuf sources
	mvn clean compile

.PHONY: generate
generate: ## Generate OpenAPI + Protobuf sources only
	mvn generate-sources

.PHONY: test
test: ## Run all tests with coverage check (JaCoCo >= 90%)
	mvn clean test

.PHONY: verify
verify: ## Full build: compile + test + coverage gate
	mvn clean verify

.PHONY: coverage
coverage: ## Generate and open JaCoCo HTML coverage report
	mvn clean test jacoco:report
	@echo "Opening coverage report..."
	@open target/site/jacoco/index.html 2>/dev/null || xdg-open target/site/jacoco/index.html 2>/dev/null || echo "Open target/site/jacoco/index.html manually"

.PHONY: package
package: ## Build production JAR (skip tests)
	mvn clean package -DskipTests

.PHONY: clean
clean: ## Clean build artifacts
	mvn clean

# ---------------------------------------------------------------------------
# Run (local development)
# ---------------------------------------------------------------------------

.PHONY: run
run: ## Start the application (dev profile, port 8081)
	mvn spring-boot:run

.PHONY: run-prod
run-prod: ## Start with production profile
	mvn spring-boot:run -Dspring-boot.run.profiles=prod

# ---------------------------------------------------------------------------
# Docker — Local Keycloak (development)
# ---------------------------------------------------------------------------

.PHONY: docker-up
docker-up: ## Start Keycloak + PostgreSQL (dev)
	docker compose -f docker/docker-compose.keycloak.yml --env-file docker/.env up -d

.PHONY: docker-down
docker-down: ## Stop Keycloak + PostgreSQL
	docker compose -f docker/docker-compose.keycloak.yml down

.PHONY: docker-reset
docker-reset: ## Stop and remove Keycloak volumes (full reset)
	docker compose -f docker/docker-compose.keycloak.yml down -v

.PHONY: docker-logs
docker-logs: ## Follow Keycloak container logs
	docker compose -f docker/docker-compose.keycloak.yml logs -f keycloak

# ---------------------------------------------------------------------------
# Docker — Production stack
# ---------------------------------------------------------------------------

.PHONY: prod-build
prod-build: ## Build the auth module Docker image
	docker compose -f docker/docker-compose.prod.yml build auth-module

.PHONY: prod-up
prod-up: ## Start all production services
	docker compose -f docker/docker-compose.prod.yml --env-file docker/.env.prod up -d

.PHONY: prod-down
prod-down: ## Stop all production services
	docker compose -f docker/docker-compose.prod.yml down

.PHONY: prod-logs
prod-logs: ## Follow auth module logs (production)
	docker compose -f docker/docker-compose.prod.yml logs -f auth-module

.PHONY: prod-status
prod-status: ## Show status of all production services
	docker compose -f docker/docker-compose.prod.yml ps

.PHONY: prod-restart
prod-restart: ## Restart auth module only
	docker compose -f docker/docker-compose.prod.yml up -d --no-deps --build auth-module

# ---------------------------------------------------------------------------
# TLS — Let's Encrypt
# ---------------------------------------------------------------------------

.PHONY: tls-init
tls-init: ## Obtain initial TLS cert (DOMAIN=auth.example.com)
	@test -n "$(DOMAIN)" || (echo "Usage: make tls-init DOMAIN=auth.example.com" && exit 1)
	docker compose -f docker/docker-compose.prod.yml run --rm certbot \
		certonly --webroot -w /var/www/certbot -d $(DOMAIN) --agree-tos --no-eff-email -m admin@$(DOMAIN)
	docker compose -f docker/docker-compose.prod.yml exec nginx nginx -s reload

.PHONY: tls-renew
tls-renew: ## Force TLS certificate renewal
	docker compose -f docker/docker-compose.prod.yml run --rm certbot renew
	docker compose -f docker/docker-compose.prod.yml exec nginx nginx -s reload

# ---------------------------------------------------------------------------
# Deploy
# ---------------------------------------------------------------------------

.PHONY: deploy
deploy: ## Deploy to VPS (VPS=user@host)
	@chmod +x deploy/deploy.sh
	./deploy/deploy.sh $(VPS)

# ---------------------------------------------------------------------------
# Help
# ---------------------------------------------------------------------------

.PHONY: help
help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
