FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY discovery-server/build.gradle.kts discovery-server/
COPY trainer-workload/build.gradle.kts trainer-workload/
RUN ./gradlew --no-daemon :discovery-server:dependencies > /dev/null

COPY discovery-server/src discovery-server/src
RUN ./gradlew --no-daemon :discovery-server:bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system eureka && useradd --system --gid eureka --no-create-home eureka
COPY --from=build /workspace/discovery-server/build/libs/discovery-server.jar app.jar
USER eureka

EXPOSE 8761
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO /dev/null http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
