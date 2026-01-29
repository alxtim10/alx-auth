# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /work
COPY --from=build /app/target/quarkus-app/ ./
EXPOSE 8080
CMD ["java","-jar","quarkus-run.jar"]
