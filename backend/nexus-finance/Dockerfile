# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY ../../gradlew ../../gradlew.bat ./
COPY ../../gradle/ ./gradle/
COPY ../../settings.gradle.kts ../../build.gradle.kts ../../gradle/libs.versions.toml ./
COPY build.gradle.kts ./nexus-finance/
COPY src/ ./nexus-finance/src/

# Copy nexus-core dependency
COPY ../nexus-core/build.gradle.kts ./nexus-core/
COPY ../nexus-core/src/ ./nexus-core/src/

RUN chmod +x gradlew && \
    ./gradlew :nexus-finance:bootJar --no-daemon -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -g 1001 nexus && adduser -u 1001 -G nexus -s /bin/sh -D nexus

WORKDIR /app
COPY --from=builder /app/nexus-finance/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1g \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=production \
  -Djdk.virtualThreadScheduler.parallelism=8"

USER nexus

HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
  CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
