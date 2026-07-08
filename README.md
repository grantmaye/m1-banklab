# m1-banklab

`m1-banklab` is a professional Java/Spring Boot banking systems lab for demonstrating backend engineering skills relevant to banking, fintech, fraud, payments, and enterprise systems.

This is not a fake banking app UI. It is a backend systems lab for learning and demonstrating Java banking infrastructure: account ledgers, transaction validation, fraud rules, auditability, and future event-driven architecture.

## What It Demonstrates

- Modular monolith architecture that can later split into services.
- Customer and account lifecycle APIs.
- Deposit, withdrawal, and transfer workflows.
- PostgreSQL persistence with Flyway migrations.
- Auditable financial actions.
- Basic fraud risk scoring.
- Request validation and clean error responses.
- OpenAPI/Swagger API documentation.
- Dockerized local development.
- JUnit 5 and Testcontainers integration tests.
- GitHub Actions CI.

## Tech Stack

- Java 21
- Spring Boot 3
- Maven
- PostgreSQL
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security starter with permissive MVP config
- Flyway
- springdoc-openapi / Swagger UI
- Docker Compose
- JUnit 5
- Testcontainers
- GitHub Actions

## Documentation

- [Architecture](docs/architecture.md)
- [Roadmap](docs/roadmap.md)
- [API Examples](docs/api-examples.md)
- [Banking Domain Notes](docs/banking-domain-notes.md)
- [Local Development](docs/local-development.md)
- [Testing Strategy](docs/testing.md)

## API Routes

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/customers` | Create customer |
| `GET` | `/api/customers/{id}` | Get customer |
| `POST` | `/api/accounts` | Create checking/savings account |
| `GET` | `/api/accounts/{id}` | Get account |
| `POST` | `/api/accounts/{id}/deposit` | Deposit funds |
| `POST` | `/api/accounts/{id}/withdraw` | Withdraw funds |
| `POST` | `/api/transfers` | Transfer between accounts |
| `GET` | `/api/accounts/{id}/transactions` | List account transactions |
| `GET` | `/api/fraud/assessments` | List fraud assessments |
| `GET` | `/api/audit/events` | List audit events |

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Quick Start

Prerequisites:

- Java 21
- Maven
- Docker Desktop

Run locally:

```sh
cp .env.example .env
docker compose up -d
mvn test
mvn spring-boot:run
```

Open Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Stop local infrastructure:

```sh
docker compose down
```

If port `5432` is already occupied:

```sh
POSTGRES_PORT=55432 docker compose up -d
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:55432/m1_banklab mvn spring-boot:run
```

## Screenshots

Add screenshots after running locally:

- `docs/screenshots/swagger-ui.png` - Swagger UI API docs.
- `docs/screenshots/postman-transfer.png` - Transfer request example.
- `docs/screenshots/fraud-assessments.png` - Fraud assessment response.
- `docs/screenshots/audit-events.png` - Audit event response.

## Why This Matters

Banking backends are less about flashy screens and more about correctness under constraints:

- Money movement must be validated and traceable.
- Account balances need consistent transaction boundaries.
- Fraud decisions must be explainable.
- Every important action needs an audit trail.
- Systems should be modular before they become distributed.

`m1-banklab` is designed to show those concerns directly in code.

## License

MIT
