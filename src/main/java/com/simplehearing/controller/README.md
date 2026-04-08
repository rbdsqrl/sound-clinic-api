# Controller Package

This package contains REST controllers that handle HTTP requests and responses for the API endpoints.

## Controllers

- **AppointmentController.java**: Handles appointment booking, retrieval, updates, and cancellation
- **AvailabilityController.java**: Manages service availability and time slots
- **BlogController.java**: Manages blog posts (CRUD operations)
- **ContactController.java**: Handles contact form submissions
- **GalleryController.java**: Manages gallery items (photos/videos)
- **HearingServiceController.java**: Manages hearing services catalog
- **PaymentController.java**: Handles payment initiation and verification via Razorpay

## Features

- RESTful API design with proper HTTP methods
- Request/response DTOs for data validation
- Exception handling with appropriate HTTP status codes
- Swagger annotations for API documentation