FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

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
