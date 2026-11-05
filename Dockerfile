FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=postgres,api
ENV REELS_KAFKA_ENABLED=true
ENV REELS_RUN_MEDIA_WORKER=false
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
