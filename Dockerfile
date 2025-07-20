# Use Eclipse Temurin with Java 21 on Alpine for a lightweight image
FROM eclipse-temurin:21-jdk-alpine

# Set working directory inside the container
WORKDIR /app

# Copy the Maven wrapper and pom.xml to leverage caching
COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline

# Copy the rest of the project files
COPY src/ src/

# Build the application, skipping tests for faster builds
RUN ./mvnw clean package -DskipTests

# Copy the generated JAR file to the working directory
COPY target/*.jar app.jar

# Expose the port your app runs on (matches server.port=8086)
EXPOSE 8086

<<<<<<< HEAD:Dockerfile
# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
=======
# Run the JAR file when the container starts
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
>>>>>>> 4f4aff3 (Dockerfile):DockerFile
