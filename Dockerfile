# Use Eclipse Temurin (OpenJDK) with Java 21
FROM eclipse-temurin:21-jdk AS builder

# Set working directory inside container
WORKDIR /app

# Copy all project files
COPY . .

# Give execution permission to Maven wrapper
RUN chmod +x mvnw

# Build the application
RUN ./mvnw clean package

# Expose the application's port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/target/utxo-tracker-0.0.1-SNAPSHOT.jar"]
