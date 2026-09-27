# Stage 1: build the fat jar. Tests are NOT run here - a container build
# has no guaranteed database to run them against, and CI is the right
# place for that gate, not the image build. `mvn -B package -DskipTests`
# still compiles everything, so a genuine compile error still fails the
# build.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the POM first and pre-fetch dependencies as their own layer, so a
# source-only change doesn't re-download the entire dependency tree.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# Stage 2: run on a minimal JRE, not the full JDK/Maven image - smaller,
# smaller attack surface.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Never run as root in the container.
RUN addgroup -S erp && adduser -S erp -G erp
COPY --from=build /app/target/*.jar app.jar
RUN chown erp:erp app.jar
USER erp

EXPOSE 8080

# Container orchestrators (Compose healthcheck, Kubernetes probes) hit
# this instead of guessing when the app is ready - actuator health is
# already exposed (see application.properties: management.endpoints.web
# .exposure.include=health,info).
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
