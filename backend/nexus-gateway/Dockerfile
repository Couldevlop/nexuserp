# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and root build files
COPY ../../gradlew ../../gradlew.bat ./
COPY ../../gradle/ ./gradle/
COPY ../../settings.gradle.kts ../../build.gradle.kts ../../gradle/libs.versions.toml ./

# Copy service-specific build file
COPY build.gradle.kts ./nexus-gateway/
COPY src/ ./nexus-gateway/src/

RUN chmod +x gradlew && \
    ./gradlew :nexus-gateway:bootJar --no-daemon -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: non-root user
RUN addgroup -g 1001 nexus && adduser -u 1001 -G nexus -s /bin/sh -D nexus

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/nexus-gateway/build/libs/*.jar app.jar

# JVM flags for production (Virtual Threads + memory)
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=production \
  -Djdk.virtualThreadScheduler.parallelism=4"

USER nexus

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
