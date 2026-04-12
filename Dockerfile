# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml first for Docker layer cache optimization
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run (lighter runtime image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add health check support
RUN apk add --no-cache curl

# Copy the built jar
COPY --from=builder /app/target/weixinspringboot-1.0.0.jar app.jar

# Environment variables for WeChat Cloud Hosting
ENV JAVA_OPTS="-Xmx256m -Xms128m"
ENV SERVER_PORT=8080

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
