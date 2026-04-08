# Main Application Package

This directory contains the core Java source code for the Simple Hearing API application.

## Package Structure

- **config/**: Spring configuration classes (security, OpenAPI, data seeding)
- **controller/**: REST API controllers handling HTTP requests
- **dto/**: Data Transfer Objects for API communication
- **entity/**: JPA entity classes representing database tables
- **enums/**: Enumeration types for status and type values
- **exception/**: Custom exception classes and global error handling
- **repository/**: Spring Data JPA repositories for data access
- **service/**: Business logic service classes
- **util/**: Utility classes for common functionality

## Main Class

- **SimpleHearingApplication.java**: The main Spring Boot application class with the `@SpringBootApplication` annotation.