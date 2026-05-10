# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Cache Maven dependencies separately from source code
# (layer is reused if pom.xml unchanged)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow

# Copy only the fat JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown taskflow:taskflow app.jar

USER taskflow

# Expose application port
EXPOSE 8080

# JVM tuning for containers:
# -XX:+UseContainerSupport  → respect Docker CPU/memory limits
# -XX:MaxRAMPercentage=75.0 → use 75% of container memory for heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
