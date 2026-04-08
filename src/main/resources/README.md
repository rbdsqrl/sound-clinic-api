# Resources Directory

This directory contains application configuration files and static resources.

## Configuration Files

- **application.yml**: Main application configuration (default profile)
- **application-dev.yml**: Development environment configuration
- **application-prod.yml**: Production environment configuration

## Configuration Details

- **Database**: H2 for dev, external DB for prod
- **Server**: Port 8080, context path '/'
- **Security**: JWT configuration, CORS settings
- **External APIs**: Razorpay payment gateway settings
- **Logging**: Log levels and output configuration

## Static Resources

- Static files (if any) served by Spring Boot
- Templates (if using server-side rendering)