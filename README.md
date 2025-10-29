# PayHint – Payment Follow-Up API

PayHint is a Spring Boot API that helps freelancers and independent contractors track invoices, monitor overdue payments, and automate reminder workflows.

## Highlights

- Automates invoice follow-up through scheduled reminders and configurable status transitions.
- Implements token-based authentication so that every API call remains stateless and secure.
- Follows a strict hexagonal architecture to keep the core domain independent from frameworks.
- Provides testable layers with clear separation between domain, application, and infrastructure concerns.

## Architecture Overview

The solution combines **Hexagonal Architecture (Ports & Adapters)** with **Domain-Driven Design (DDD)** patterns:

- **Domain layer** exposes business objects and repository ports without any Spring dependency.
- **Application layer** orchestrates use cases, translates inputs via DTOs, and delegates to domain services.
- **Infrastructure layer** adapts the domain to technical concerns such as persistence, HTTP, and security.

### Package Structure

```
src/main/java/com/payhint/api
├── PayHintApplication.java
├── application/
│   ├── billing/
│   ├── crm/
│   │   ├── dto/
│   │   ├── mapper/
│   │   ├── port/
│   │   └── service/
│   └── shared/
│       ├── exceptions/
│       └── ValueObjectMapper.java
├── domain/
│   ├── billing/
│   │   ├── model/
│   │   └── repository/
│   ├── crm/
│   │   ├── model/
│   │   ├── repository/
│   │   └── valueobjects/
│   └── shared/
│       └── annotation/
└── infrastructure/
    ├── configuration/
    ├── persistence/
    │   └── jpa/
    │       ├── billing/
    │       │   ├── adapter/
    │       │   ├── entity/
    │       │   ├── mapper/
    │       │   └── repository/
    │       └── crm/
    │           ├── adapter/
    │           ├── entity/
    │           ├── mapper/
    │           └── repository/
    ├── security/
    ├── utils/
    └── web/
        ├── controller/
        └── exception/

src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
├── application-prod.yml
├── application.template.yml
└── static/
    └── database_schema.sql
```

## Technology Stack

- Java 21
- Spring Boot 3.x (web, data, validation, actuator)
- PostgreSQL with Spring Data JPA and Hibernate
- Spring Security 6 with JWT for stateless authentication
- MapStruct for DTO ↔ domain mapping
- Lombok to reduce boilerplate
- Maven for build orchestration

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL 14 or later
- Maven 3.8+

### Configure the Environment

1. Copy `src/main/resources/application.template.yml` to `application.yml` (or the profile-specific file you plan to use) and adjust credentials, JWT secrets, and mail settings.
2. Create a PostgreSQL database and user with access rights:

   ```sql
   CREATE DATABASE payhint_dev;
   CREATE USER payhint_user WITH PASSWORD 'payhint_password';
   GRANT ALL PRIVILEGES ON DATABASE payhint_dev TO payhint_user;
   ```

3. Run `src/main/resources/static/database_schema.sql` to provision schema objects.

### Run the Application

```bash
# Start the API with the dev profile (default)
./mvnw spring-boot:run

# Package the application
./mvnw clean package -DskipTests
java -jar target/*.jar
```

### Execute Tests

```bash
./mvnw test
```

## Operational Notes

- All non-authentication endpoints are protected by JWT; obtain a token via the authentication controller before accessing invoice or client resources.
- Metrics, health checks, and readiness endpoints are exposed through Spring Boot Actuator when enabled via configuration.
- Follow the Hexagonal Architecture guidelines when contributing: introduce new domain logic in `domain`, define ports in `application`, and implement adapters within `infrastructure`.
