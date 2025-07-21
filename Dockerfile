# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy and build the project
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port
EXPOSE 8086

<<<<<<< HEAD:Dockerfile
<<<<<<< HEAD:Dockerfile
# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
=======
# Run the JAR file when the container starts
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
>>>>>>> 4f4aff3 (Dockerfile):DockerFile
=======
# Run the JAR
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
>>>>>>> f3ac01a (Error Resolved - Parcipant Quiz Error):DockerFile
