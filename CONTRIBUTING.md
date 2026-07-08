# Contributing

Contributions are welcome. This project is a banking systems lab, so changes should favor correctness, auditability, and clear tradeoffs over feature volume.

## Local Development

```sh
cp .env.example .env
docker compose up -d
mvn test
mvn spring-boot:run
```

## Pull Request Guidelines

- Keep business logic in services, not controllers.
- Use DTOs at API boundaries.
- Use `BigDecimal` for money.
- Add or update Flyway migrations for schema changes.
- Include tests for ledger, fraud, audit, or validation behavior.
- Update the README when API behavior changes.

## Banking Safety Notes

This is an educational lab, not production banking software. Do not use it to hold real customer data, move real money, or make real credit/fraud decisions.
