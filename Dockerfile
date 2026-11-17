# Gym CRM. Built from the repository root (the Gradle build includes the other services' build files).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Build files first: the dependency layer is reused until one of them changes.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY discovery-server/build.gradle.kts discovery-server/
COPY trainer-workload/build.gradle.kts trainer-workload/
RUN ./gradlew --no-daemon :dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon :bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system gymcrm && useradd --system --gid gymcrm --no-create-home gymcrm \
    && mkdir /app/logs && chown gymcrm:gymcrm /app/logs
COPY --from=build /workspace/build/libs/gym-crm.jar app.jar
USER gymcrm
ENV SPRING_PROFILES_ACTIVE=standalone
ENV LOGGING_FILE_NAME=/app/logs/gym-crm.log

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
