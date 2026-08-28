# Stage 1: Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Maven descriptor & wrapper
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies offline for layer caching
RUN ./mvnw dependency:go-offline -B || true

# Copy source code & build artifact
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Security: Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
