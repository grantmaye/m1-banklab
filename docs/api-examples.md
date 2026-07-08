# API Examples

Run the app first:

```sh
docker compose up -d
mvn spring-boot:run
```

Base URL:

```text
http://localhost:8080
```

## Create Customer

```sh
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Maya",
    "lastName": "Chen",
    "email": "maya.chen@example.com",
    "phone": "+1-336-555-1212"
  }'
```

## Get Seed Customer

```sh
curl http://localhost:8080/api/customers/11111111-1111-1111-1111-111111111111
```

## Create Account

```sh
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "11111111-1111-1111-1111-111111111111",
    "type": "CHECKING",
    "openingBalance": "500.00"
  }'
```

Valid account types:

- `CHECKING`
- `SAVINGS`

## Get Seed Account

```sh
curl http://localhost:8080/api/accounts/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
```

## Deposit

```sh
curl -X POST http://localhost:8080/api/accounts/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "6000.00",
    "description": "payroll funding"
  }'
```

Expected side effects:

- Account balance increases.
- Transaction is inserted.
- Fraud assessment is inserted.
- Audit events are inserted.

## Withdraw

```sh
curl -X POST http://localhost:8080/api/accounts/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "125.00",
    "description": "atm withdrawal"
  }'
```

## Failed Withdrawal

```sh
curl -X POST http://localhost:8080/api/accounts/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "999999.00",
    "description": "oversized withdrawal"
  }'
```

Expected side effects:

- Account balance does not change.
- Failed transaction is inserted.
- Fraud assessment is inserted.
- `WITHDRAWAL_FAILED` audit event is inserted.

## Transfer

```sh
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "targetAccountId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
    "amount": "250.00",
    "description": "rent split"
  }'
```

Expected side effects:

- Source account balance decreases.
- Target account balance increases.
- Transaction is inserted.
- Fraud engine checks amount, velocity, and new-payee status.
- Audit events are inserted.

## Account Transactions

```sh
curl http://localhost:8080/api/accounts/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/transactions
```

## Fraud Assessments

```sh
curl http://localhost:8080/api/fraud/assessments
```

## Audit Events

```sh
curl http://localhost:8080/api/audit/events
```

## Swagger And OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```sh
curl http://localhost:8080/v3/api-docs
```
