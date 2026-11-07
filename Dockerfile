# ==============================================================================
# Multi-stage Dockerfile for Java 21 Spring Boot Application
# Compatible with Render, Railway, Fly.io, AWS App Runner, Google Cloud Run
# ==============================================================================

# Stage 1: Build the application with Maven
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper / build files first for optimal Docker layer caching
COPY pom.xml .
COPY src ./src

# Install Maven and build the production executable jar
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# Stage 2: Production Minimal JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-privileged system user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled jar from builder stage
COPY --from=builder /build/target/qr-attendance-1.0.0.jar app.jar

# Set ownership to non-root user
RUN chown -R appuser:appgroup /app
USER appuser

# Expose standard application port
EXPOSE 8080

# Environment defaults (can be overridden by cloud platform environment variables)
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=supabase \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

# Launch Spring Boot with container-aware memory flags
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]

