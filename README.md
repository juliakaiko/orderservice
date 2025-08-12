# OrderService
OrderService is a backend service designed to manage and process customer orders, 
built on Spring Boot 3.3.4 using PostgreSQL, Liquibase, and OpenAPI (Swagger).

## 🚀 Key Features
- Order management (CRUD operations).
- Automated database updates via Liquibase.
- API documentation via Swagger UI.
- Docker support (build and deployment).

## 🧩 Architecture
This project follows a layered architecture:

- **Controller Layer**: Handles HTTP requests and maps to services.
- **Service Layer**: Contains business logic.
- **Repository Layer**: Interfaces with PostgreSQL using Spring Data JPA.
- **DTO + Mapper Layer**: Used for clean data transfer and separation from entities.
- **Global Exception Handling**: Uniform response structure for all errors.

## ⚙️ Technologies
- Java 21
- Spring Boot 3.3.4 (Web, Data JPA, Validation, Redis)
- PostgreSQL (primary database)
- Liquibase (migrations)
- MapStruct (DTO mapping)
- Lombok (reducing boilerplate code)
- Feign: Declarative REST client used to simplify HTTP API calls to userservice.
- SpringDoc OpenAPI (API documentation)
- Docker for containerization

## 🧪 Testing
- Unit Testing: JUnit 5 + Mockito for mocking dependencies.
- Containerized Integration Testing: Testcontainers for running PostgreSQLin Docker containers during integration tests.

## 📩 Contacts
**Author:** Yuliya Kaiko
**Email:** yuliya.kaiko@innowise.com

