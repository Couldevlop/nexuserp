# NexusERP — nexus-core
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew :backend:services:nexus-core:bootJar -x test --no-daemon --build-cache

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S nexus && adduser -S nexus -G nexus
WORKDIR /app
COPY --from=builder /workspace/backend/services/nexus-core/build/libs/*.jar app.jar
RUN chown nexus:nexus app.jar
USER nexus
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
