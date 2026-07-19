# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY customer-service/pom.xml customer-service/pom.xml
COPY product-catalog-service/pom.xml product-catalog-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY subscription-service/pom.xml subscription-service/pom.xml
COPY usage-service/pom.xml usage-service/pom.xml
COPY billing-service/pom.xml billing-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ticket-service/pom.xml ticket-service/pom.xml

COPY api-gateway/src api-gateway/src
RUN --mount=type=cache,target=/root/.m2 mvn -pl api-gateway -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
