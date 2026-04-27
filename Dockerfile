# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy parent pom first so Maven can resolve the module structure
COPY pom.xml .
COPY sky-common/pom.xml  sky-common/
COPY sky-pojo/pom.xml    sky-pojo/
COPY sky-server/pom.xml  sky-server/

# Download dependencies in a separate layer (cached unless poms change)
RUN mvn dependency:go-offline -q

# Copy all source code
COPY sky-common/src  sky-common/src
COPY sky-pojo/src    sky-pojo/src
COPY sky-server/src  sky-server/src

# Build, skip tests (tests run in CI, not in the image build)
RUN mvn clean package -pl sky-server -am -DskipTests -q

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Non-root user for security — good practice to mention in interviews
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /build/sky-server/target/sky-server-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
