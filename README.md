# LifeStream Blog System

A personal blog system based on Spring Boot, providing article publishing, image archiving, comments and other functions.

## Technology Stack

### Backend
- Java 11+
- Spring Boot 2.7.x
- Spring Security
- Spring Data JPA
- MySQL/PostgreSQL
- Maven

### Frontend
- Thymeleaf
- Bootstrap 5
- jQuery
- Font Awesome
- Summernote (Rich Text Editor)

## Features

- User Authentication & Authorization
    - JWT-based authentication
    - Role management (Admin, Regular User)

- Article Management
    - Create/edit/delete articles
    - Rich text editor support
    - Tag management
    - Article search
    - Archive by tags or dates

- Comment System
    - Post comments and replies
    - Comment management

- Image Management
    - Album creation and management
    - Image upload and display
    - Image viewer

- Profile Settings
    - User profile configuration
    - Avatar settings

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── yourblog/
│   │           ├── config/         # Configuration classes
│   │           ├── controller/     # Controllers
│   │           ├── entity/         # Entity classes
│   │           ├── exception/      # Exception handling
│   │           ├── payload/        # Request/Response objects
│   │           ├── repository/     # Data access layer
│   │           ├── security/       # Security configuration
│   │           ├── service/        # Business logic layer
│   │           └── util/           # Utility classes
│   └── resources/
│       ├── static/                 # Static resources
│       │   ├── css/                # CSS files
│       │   ├── js/                 # JavaScript files
│       │   └── images/             # Image assets
│       ├── templates/              # Thymeleaf templates
│       │   ├── albums/             # Album-related pages
│       │   ├── auth/               # Authentication pages
│       │   ├── posts/              # Article-related pages
│       │   ├── profile/            # Profile pages
│       │   └── error/              # Error pages
│       └── application.properties  # Application configuration
└── test/                           # Test code
```

## Installation & Running

### Prerequisites

- JDK 11+
- Maven 3.6+
- MySQL 8+ or PostgreSQL 12+

### Steps

1. Clone repository
   ```bash
   git clone https://github.com/yourusername/yourblog.git
   cd yourblog
   ```

2. Configure database
   Modify database connection in `application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/yourblog_db?useSSL=false&serverTimezone=UTC
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. Build project
   ```bash
   mvn clean package
   ```

4. Run application
   ```bash
   java -jar target/yourblog-0.0.1-SNAPSHOT.jar
   ```
   Or use Maven:
   ```bash
   mvn spring-boot:run
   ```

5. Access application
   Visit `http://localhost:8080` in browser

## Initial Accounts

The system will automatically create test accounts after startup (Development environment only):

- Admin account:
    - Username: admin
    - Password: admin123

- Regular user account:
    - Username: user
    - Password: user123

## Development Guide

### Adding New Features

1. Create new entity class in `entity` package
2. Create corresponding Repository interface in `repository` package
3. Implement business logic in `service` package
4. Create new controller in `controller` package
5. Add required Thymeleaf templates in `templates` directory

### Configuration

- Development environment: `application-dev.properties`
- Production environment: `application-prod.properties`

Specify environment using:
```bash
java -jar yourblog.jar --spring.profiles.active=prod
```

## Deployment

### Using Docker

1. Build Docker image
   ```bash
   docker build -t yourblog .
   ```

2. Run container
   ```bash
   docker run -p 8080:8080 yourblog
   ```

### Traditional Deployment

1. Configure production environment
   ```bash
   java -jar yourblog.jar --spring.profiles.active=prod
   ```

2. Use Nginx as reverse proxy (Recommended)

## Contribution Guide

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add some amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Create Pull Request

## License

This project is licensed under the PC-NC License

Copyright (c) 2025 PrincipleCreativityOrg.

Licensed under the PrincipleCreativity Non-Commercial License (PC-NC).
