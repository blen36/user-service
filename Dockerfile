# Stage 1: compile and package the application with Java 21.
FROM maven:3.10.0-eclipse-temurin-21-noble AS build
WORKDIR /build

COPY pom.xml .
COPY src/main ./src/main
# Run the complete test suite on the host before building the image.
# Testcontainers tests require Docker and are not run inside this build.
RUN mvn -B -ntp -DskipTests package

# Stage 2: run only the packaged application and Java runtime.
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
