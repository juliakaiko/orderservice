FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the JAR file into the container
COPY target/orderservice-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
