# PayHint – Payment follow-up API

PayHint is a Spring Boot API that helps freelancers and independent contractors track invoices, monitor overdue payments, and automate reminder workflows.

## Highlights

- Automates invoice follow-up through scheduled reminders and configurable status transitions.
- Implements token-based authentication so that every API call remains stateless and secure.
- Follows a strict hexagonal architecture to keep the core domain independent from frameworks.
- Provides testable layers with clear separation between domain, application, and infrastructure concerns.

## Architecture overview

The solution combines **Hexagonal Architecture (Ports & Adapters)** with **Domain-Driven Design (DDD)** patterns:

- **Domain layer** exposes business objects and repository ports without any Spring dependency.
- **Application layer** orchestrates use cases, translates inputs via DTOs, and delegates to domain services.
- **Infrastructure layer** adapts the domain to technical concerns such as persistence, HTTP, and security.

### Package structure

```
src/main/java/com/payhint/api
├── PayHintApplication.java
├── application/
│   ├── billing/
│   │   ├── dto/
│   │   ├── mapper/
│   │   ├── usecase/
│   │   └── service/
│   ├── crm/
│   │   ├── ### same as <billing>
│   └── shared/
│       ├── exceptions/
│       └── ValueObjectMapper.java
├── domain/
│   ├── billing/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/
|   |   └── valueobjects/
│   ├── crm/
│   │   ├── ### same as <billing>
│   └── shared/
│       └── annotation/
└── infrastructure/
   ├── billing/
   │   └── persistence/
   │   │   └── jpa/
   │   │       ├── adapter/
   │   │       ├── entity/
   │   │       ├── mapper/
   │   │       └── repository/
   │   └── web/
   |        └── adapter/
   ├── crm/
   |   ├── ### same as <billing>
   └── shared/
      ├── configuration/
      ├── exception/
      ├── security/
      └── utils/


src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
├── application-prod.yml
├── application.template.yml
└── static/
   └── database_schema.sql

```

## Technology stack

- Java 21
- Spring Boot 3.x (web, data, validation, actuator)
- PostgreSQL with Spring Data JPA and Hibernate
- Spring Security 6 with JWT for stateless authentication
- MapStruct for DTO ↔ domain mapping
- Lombok to reduce boilerplate
- JUnit 5 and Mockito for tests
- Maven for build orchestration

## Getting started

### Prerequisites

- JDK 21
- PostgreSQL 14 or later
- Maven 3.8+

### Configure the environment

1. Copy `src/main/resources/application.template.yml` to `src/main/resources/application.yml` (or create a profile-specific file) and update the following placeholders:

   - datasource URL, username and password
   - `jwt.secret` and token properties
   - mail settings used for reminder notifications

2. Create a PostgreSQL database and user (example):

   ```sql
   CREATE DATABASE payhint_dev;
   CREATE USER payhint_user WITH PASSWORD 'payhint_password';
   GRANT ALL PRIVILEGES ON DATABASE payhint_dev TO payhint_user;
   ```

3. (Optional) Run `src/main/resources/static/database_schema.sql` to provision sample schema objects used by the project.

### Run the application

```bash
# Build and run (dev profile by default)
./mvnw spring-boot:run

# Package the application
./mvnw clean package -DskipTests
java -jar target/*.jar
```

### Execute tests

```bash
./mvnw test
```

## Operational notes

- **Security:** All non-authentication endpoints require a valid JWT token. Use the authentication endpoints under `/api/auth` to register or obtain tokens.
- **Profiles & configuration:** Use `application-dev.yml`, `application-test.yml`, and `application-prod.yml` for environment-specific settings.
- **Actuator:** Metrics and health endpoints are available when enabled in configuration.
- **Contributing:** For new features add domain logic under `src/main/java/com/payhint/api/domain`, expose use cases in `application`, and implement adapters in `infrastructure`.
