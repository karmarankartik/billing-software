# ============================================================
# WATER BILLING SYSTEM - DOCKERFILE
# ============================================================
#
# IMPORTANT:
# The Dockerfile intentionally uses *.jar instead of a hard-coded
# JAR filename. This prevents the Dockerfile from breaking when
# the Maven artifact name or version changes.
#
# WARNING:
# Make sure the /target directory contains only the application
# JAR when the Docker image is built. If multiple JAR files are
# present, the wildcard may match more than one file.
#
# ============================================================


# ============================================================
# Build Stage
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven configuration first for better Docker layer caching
COPY pom.xml .

# Download Maven dependencies
RUN mvn dependency:go-offline -B

# Copy application source
COPY src ./src

# Build Spring Boot application
RUN mvn clean package -DskipTests


# ============================================================
# Runtime Stage
# ============================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the generated Spring Boot application JAR.
# Do not hard-code the Maven artifact name/version here.
COPY --from=build /app/target/*.jar app.jar

# Application port
EXPOSE 8080

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]
