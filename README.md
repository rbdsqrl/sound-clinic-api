# Simple Hearing API

![CI](https://github.com/rbdsqrl/sound-clinic-api/actions/workflows/docker-build.yml/badge.svg) ![Coverage](https://rbdsqrl.github.io/sound-clinic-api/)

A REST API for a hearing services platform built with Spring Boot 3.3.4 and Java 21.

## Prerequisites

- Java 21+
- Maven 3.6+

## Local Setup

```bash
git clone https://github.com/rbdsqrl/sound-clinic-api.git
cd backend
mvn clean install
mvn spring-boot:run
```

Application runs on `http://localhost:8080`.

## API Documentation

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

**API Contract:** See `swagger.yaml` in the repository root.

## Testing

```bash
mvn test
```

## Docker

Build the JAR first:

```bash
mvn clean package -DskipTests
```

Then build and run the image:

```bash
docker build --tag simple-hearing-api:latest .
docker run --rm -p 8080:8080 simple-hearing-api:latest
```

## CI/CD

GitHub Actions workflow:
- Runs tests and generates JaCoCo coverage
- Builds Docker image and publishes artifacts
- Publishes coverage report to GitHub Pages
- Creates build tags on successful main push

Reports: `https://rbdsqrl.github.io/sound-clinic-api/`

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/simplehearing/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/            # Data Transfer Objects
│   │   │   ├── entity/         # JPA entities
│   │   │   ├── enums/          # Enum classes
│   │   │   ├── exception/      # Custom exceptions
│   │   │   ├── repository/     # Data repositories
│   │   │   ├── service/        # Business logic
│   │   │   └── util/           # Utility classes
│   │   └── resources/          # Application properties
│   └── test/                   # Unit tests
├── target/                     # Build output
├── pom.xml                     # Maven configuration
└── README.md                   # This file
```

## Contributing

1. Create a feature branch
2. Make your changes
3. Add tests
4. Submit a pull request

## License

[Add license information here]