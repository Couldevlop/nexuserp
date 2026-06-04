# NexusERP — nexus-hr (Spring Boot 3 / Java 21)

FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY backend/services/nexus-core/pom.xml        backend/services/nexus-core/pom.xml
COPY backend/services/nexus-gateway/pom.xml     backend/services/nexus-gateway/pom.xml
COPY backend/services/nexus-auth/pom.xml        backend/services/nexus-auth/pom.xml
COPY backend/services/nexus-finance/pom.xml     backend/services/nexus-finance/pom.xml
COPY backend/services/nexus-procurement/pom.xml backend/services/nexus-procurement/pom.xml
COPY backend/services/nexus-inventory/pom.xml   backend/services/nexus-inventory/pom.xml
COPY backend/services/nexus-sales/pom.xml       backend/services/nexus-sales/pom.xml
COPY backend/services/nexus-hr/pom.xml          backend/services/nexus-hr/pom.xml
COPY backend/services/nexus-production/pom.xml  backend/services/nexus-production/pom.xml
COPY backend/services/nexus-notification/pom.xml backend/services/nexus-notification/pom.xml
COPY backend/services/nexus-import/pom.xml      backend/services/nexus-import/pom.xml
COPY backend/services/nexus-reporting/pom.xml   backend/services/nexus-reporting/pom.xml
COPY backend/services/nexus-payment/pom.xml    backend/services/nexus-payment/pom.xml
COPY backend/services/nexus-config/pom.xml       backend/services/nexus-config/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml dependency:go-offline -pl backend/services/nexus-hr -am -q 2>/dev/null || true

COPY backend/services/nexus-core/src backend/services/nexus-core/src
COPY backend/services/nexus-hr/src   backend/services/nexus-hr/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml package -pl backend/services/nexus-hr -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S nexus && adduser -S nexus -G nexus

WORKDIR /app

COPY --from=builder /workspace/backend/services/nexus-hr/target/*.jar app.jar
RUN chown nexus:nexus app.jar

USER nexus

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseZGC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
