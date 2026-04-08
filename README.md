# Simple Hearing API

A comprehensive REST API for a hearing services platform built with Spring Boot. This application manages appointments, hearing services, blog posts, gallery items, contact messages, and payment processing.

## Features

- **Appointment Management**: Schedule and manage hearing appointments
- **Service Catalog**: Manage hearing services with pricing and descriptions
- **Blog System**: Create and manage blog posts about hearing health
- **Gallery**: Display photos and videos of services/facilities
- **Contact System**: Handle customer inquiries
- **Payment Integration**: Process payments via Razorpay
- **Security**: JWT-based authentication and authorization

## Tech Stack

- **Java 17**
- **Spring Boot 3.x**
- **Spring Data JPA** (with Hibernate)
- **H2 Database** (for development)
- **Spring Security** (with JWT)
- **Maven** (build tool)
- **OpenAPI/Swagger** (API documentation)

## Prerequisites

- Java 17 or higher
- Maven 3.6+ (will be installed via Homebrew if needed)
- Git

## Repository Setup

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd backend
   ```

2. **Install dependencies:**
   ```bash
   mvn clean install
   ```

## Application Startup

1. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Access the application:**
   - API Base URL: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - API Docs: `http://localhost:8080/v3/api-docs`

## Configuration

The application uses different profiles:

- **dev** (default): Uses H2 in-memory database
- **prod**: Uses production database (configure in `application-prod.yml`)

To run with a specific profile:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

## Database

- **Development**: H2 in-memory database (data persists during runtime)
- **Production**: PostgreSQL/MySQL (configure connection in `application-prod.yml`)

Database schema is automatically created/updated via Hibernate.

## API Endpoints

### Appointments
- `GET /api/appointments` - List appointments
- `POST /api/appointments` - Create appointment
- `GET /api/appointments/{id}` - Get appointment details
- `PUT /api/appointments/{id}` - Update appointment
- `DELETE /api/appointments/{id}` - Cancel appointment

### Services
- `GET /api/services` - List hearing services
- `POST /api/services` - Create service (admin)
- `GET /api/services/{id}` - Get service details
- `PUT /api/services/{id}` - Update service (admin)

### Blog
- `GET /api/blog` - List blog posts
- `POST /api/blog` - Create blog post (admin)
- `GET /api/blog/{id}` - Get blog post

### Gallery
- `GET /api/gallery` - List gallery items
- `POST /api/gallery` - Add gallery item (admin)

### Contact
- `POST /api/contact` - Submit contact message

### Payments
- `POST /api/payments/initiate` - Initiate payment
- `POST /api/payments/verify` - Verify payment

## Testing

Run unit tests:
```bash
mvn test
```

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