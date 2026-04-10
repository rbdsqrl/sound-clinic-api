# Runtime image: use a pre-built Spring Boot JAR
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY target/simple-hearing-api-1.0.0.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
