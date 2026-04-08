# Test Directory

This directory contains unit and integration tests for the application.

## Test Classes

- **SimpleHearingApplicationTests.java**: Main application context test

## Testing Strategy

- Unit tests for service classes
- Integration tests for controllers
- Repository tests with test database
- Mock external dependencies
- Test coverage for critical business logic

## Running Tests

```bash
mvn test
```

## Test Configuration

- Uses H2 in-memory database for tests
- Test-specific application properties
- Mock beans for external services