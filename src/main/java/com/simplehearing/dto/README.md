# DTO Package

This package contains Data Transfer Objects used for API communication between client and server.

## Subdirectories

- **request/**: DTOs for incoming API requests (validation and data binding)
- **response/**: DTOs for outgoing API responses (data serialization)

## Purpose

DTOs help:
- Validate incoming data
- Control what data is exposed in responses
- Decouple API contracts from internal entity models
- Improve API maintainability and versioning