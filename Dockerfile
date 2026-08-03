# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# Hạn chế RAM của Maven lúc Build để không bị sập trên Render
ENV MAVEN_OPTS="-Xmx256m"
# Download dependencies first to cache them
RUN mvn dependency:go-offline -B
# Copy the source code and build the application
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the application with optimized settings for Render Free Tier (512MB RAM, 0.1 CPU) to prevent OOM
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xss256k", "-XX:MaxRAMPercentage=60.0", "-XX:MaxMetaspaceSize=128m", "-jar", "app.jar"]