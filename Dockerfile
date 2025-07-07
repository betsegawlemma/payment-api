FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the application JAR (update the name as needed)
COPY build/libs/PaymentApi-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your application runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]