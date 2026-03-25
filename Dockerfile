FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

# Ensure Maven wrapper is executable
RUN chmod +x mvnw

# Build the project
RUN ./mvnw clean package -DskipTests

# Run the jar
CMD ["java", "-jar", "target/rollbasedlogin-0.0.1-SNAPSHOT.jar"]