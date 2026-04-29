# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy the Maven project
COPY ollama-gateway/ .

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR from builder
COPY --from=builder /build/target/ollama-gateway-1.0.0.jar app.jar

# Create a non-root user for security
RUN addgroup -S appuser && adduser -S appuser -G appuser
USER appuser

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
