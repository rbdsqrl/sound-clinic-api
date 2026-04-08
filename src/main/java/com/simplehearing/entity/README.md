# Entity Package

This package contains JPA entity classes that represent database tables.

## Entities

- **Appointment.java**: Represents customer appointments
- **BlogPost.java**: Represents blog articles
- **ContactMessage.java**: Represents contact form submissions
- **GalleryItem.java**: Represents gallery photos/videos
- **HearingService.java**: Represents available hearing services
- **Payment.java**: Represents payment transactions

## Features

- JPA annotations for ORM mapping
- Relationships between entities (One-to-One, One-to-Many, Many-to-One)
- Audit fields (created_at, updated_at)
- Proper indexing and constraints
- Enum fields for status and type values