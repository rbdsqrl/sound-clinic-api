# ── Build stage ────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Download dependencies first — this layer is cached unless pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn -B package -DskipTests -q

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/target/simple-hearing-api-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# MaxRAMPercentage only bounds the heap — it says nothing about Metaspace, thread stacks, or
# code cache, which the JVM is otherwise free to grow without limit. On Render's 512MB free
# tier, 75% left too little headroom for everything else a Spring Boot + Hibernate + AWS SDK +
# OpenPDF app needs outside the heap (Metaspace alone commonly runs 90-150MB), which is the
# likely cause of hitting the instance's memory ceiling. Lowered to 50% and Metaspace is now
# capped explicitly (192m — 128m proved too tight and threw OutOfMemoryError: Metaspace on
# first use of lazily-loaded paths such as the analytics endpoints) instead of left to grow unbounded; server.tomcat.threads.max (see
# application-prod.yml) bounds the other big uncapped lever, worst-case thread-stack memory.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=50.0", \
  "-XX:MaxMetaspaceSize=192m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
