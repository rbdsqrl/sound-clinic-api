# ── Runtime image ─────────────────────────────────────────────────────────────
# The JAR is built by the CI workflow (mvn package) before docker build runs.
# .dockerignore passes target/*.jar through so we just copy it here.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY target/simple-hearing-api-*.jar app.jar

# Always run with the prod Spring profile inside the container.
# Override by passing -e SPRING_PROFILES_ACTIVE=local at docker run time if needed.
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
