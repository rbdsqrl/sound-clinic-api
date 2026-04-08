# Exception Package

This package contains custom exception classes and global exception handling.

## Classes

- **GlobalExceptionHandler.java**: Global exception handler using @ControllerAdvice
- **PaymentVerificationException.java**: Exception for payment verification failures
- **ResourceNotFoundException.java**: Exception for missing resources
- **SlotUnavailableException.java**: Exception for unavailable appointment slots

## Features

- Centralized error handling
- Consistent error response format
- Appropriate HTTP status codes
- Detailed error messages for debugging
- Logging of exceptions