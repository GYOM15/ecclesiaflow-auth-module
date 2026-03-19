# =============================================================================
# EcclesiaFlow Auth Module — Multi-stage Dockerfile
# =============================================================================
# Stage 1: Build the application with Maven
# Stage 2: Run with a minimal JRE image
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1 — Build
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Cache Maven dependencies (layer caching)
COPY pom.xml ./
COPY src/main/proto/ src/main/proto/
COPY src/main/resources/api/ src/main/resources/api/
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B -q

# Copy source and build
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B -q \
    && mv target/ecclesiaflow-auth-module-*.jar target/app.jar

# ---------------------------------------------------------------------------
# Stage 2 — Runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8081 9090

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8081/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
