# Multi-stage build for Spring Boot 3 / Java 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Minimal Alpine JRE runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S rimsgroup && adduser -S rimsuser -G rimsgroup
COPY --from=build /app/target/rental-item-management-1.0.0.jar app.jar
RUN mkdir -p /app/data && chown -R rimsuser:rimsgroup /app
USER rimsuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
