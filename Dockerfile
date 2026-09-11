FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# Dependencies first, so they stay cached when only the source changes.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet || true

COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd --system app && useradd --system --gid app --home /app app

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown -R app:app /app

USER app
EXPOSE 8080

# Heap sized from the container memory limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
