FROM eclipse-temurin:21-jdk-alpine

# Install Maven
RUN apk add --no-cache maven

WORKDIR /app

# Copy POM and source
COPY pom.xml .
COPY src src

# Build
RUN mvn package -DskipTests

# Config
ENV DB_PATH=/data/plotarmor-data \
    SERVER_PORT=8080 \
    MAX_CACHE_SIZE=1000 \
    VOCABULARY_SIZE=10000 \
    BATCH_SIZE=1000

VOLUME /data/plotarmor-data
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/target/plotarmor-1.0.0.jar"]