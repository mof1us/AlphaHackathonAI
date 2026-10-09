# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew
RUN ./gradlew :server:bootJar :authorization-server:bootJar :temporal-runner:bootJar \
    --no-daemon --console=plain --max-workers=2

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
WORKDIR /app
USER app
ENTRYPOINT ["java", "-jar", "/app/application.jar"]

FROM runtime AS server
COPY --from=build --chown=app:app /workspace/server/build/libs/server.jar /app/application.jar
EXPOSE 8080

FROM runtime AS authorization-server
COPY --from=build --chown=app:app /workspace/authorization-server/build/libs/authorization-server.jar /app/application.jar
EXPOSE 9000

FROM runtime AS temporal-runner
COPY --from=build --chown=app:app /workspace/temporal-runner/build/libs/temporal-runner.jar /app/application.jar
