# Stage 1: Build
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

# Copy Gradle wrapper and config first (dependency caching layer)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies (cached unless build files change)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN ./gradlew clean bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built jar from build stage
COPY --from=builder /build/build/libs/*.jar /app/app.jar

# Set ownership to appuser
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port 8080
EXPOSE 8080

# Set JAVA_OPTS environment variable with sensible defaults
ENV JAVA_OPTS=""

# Use exec form for proper signal handling
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
