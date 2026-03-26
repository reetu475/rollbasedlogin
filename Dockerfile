# Use JDK 21 for build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy project files
COPY . .

# Ensure Maven wrapper is executable
RUN chmod +x mvnw

# Build the JAR
RUN ./mvnw clean package -DskipTests

# Runtime image
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/rollbasedlogin-0.0.1-SNAPSHOT.jar app.jar

# Expose the dynamic port (Render sets PORT)
EXPOSE 8080

# Run the JAR
CMD ["java", "-jar", "app.jar"]