# Use an official JDK image
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy project files
COPY . .

# Build with Maven wrapper
RUN ./mvnw clean package -DskipTests

# Run the jar
CMD ["java", "-jar", "target/rollbasedlogin-0.0.1-SNAPSHOT.jar"]