# Trainer Workload service. Build from the repository root:
#   docker build -f trainer-workload/Dockerfile -t trainer-workload:1.0 .
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY discovery-server/build.gradle.kts discovery-server/
COPY trainer-workload/build.gradle.kts trainer-workload/
RUN ./gradlew --no-daemon :trainer-workload:dependencies > /dev/null

COPY trainer-workload/src trainer-workload/src
RUN ./gradlew --no-daemon :trainer-workload:bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system workload && useradd --system --gid workload --no-create-home workload \
    && mkdir /app/logs && chown workload:workload /app/logs
COPY --from=build /workspace/trainer-workload/build/libs/trainer-workload.jar app.jar
USER workload
# Settings come from environment variables: EUREKA_URL, WORKLOAD_JWT_SECRET, SERVER_PORT.
ENV LOGGING_FILE_NAME=/app/logs/trainer-workload.log

EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO /dev/null http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
