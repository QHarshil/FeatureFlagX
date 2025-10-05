# ==============================
# 🔨 Stage 1: Build
# ==============================
FROM maven:3.9.5-eclipse-temurin-11 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================
# 🚀 Stage 2: Runtime
# ==============================
FROM eclipse-temurin:11-jre-alpine

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy the built JAR
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# ==============================
# ✅ Environment configuration
# ==============================
# Activate test profile (uses H2, disables Redis)
ENV SPRING_PROFILES_ACTIVE=test

# Expose port
EXPOSE 8080

# Health check for CI/CD
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
