# Architecture

`m1-banklab` starts as a modular monolith. The goal is to keep deployment simple while making domain boundaries explicit enough that modules can later split into services.

## System Context

```mermaid
flowchart LR
    Client[API client / Swagger / Postman] --> API[Spring Boot REST API]
    API --> DB[(PostgreSQL)]
    API --> OpenAPI[OpenAPI docs]
    CI[GitHub Actions] --> Tests[JUnit + Testcontainers]
    Tests --> DBTest[(Ephemeral PostgreSQL)]
```

## Module Map

```mermaid
flowchart LR
    API[REST API Controllers] --> Customers[customers]
    API --> Accounts[accounts]
    API --> Transactions[transactions]
    API --> Fraud[fraud]
    API --> Audit[audit]

    Accounts --> Customers
    Accounts --> Transactions
    Transactions --> Fraud
    Transactions --> Audit
    Fraud --> Audit
    Fraud --> Notifications[notifications]

    Customers --> DB[(PostgreSQL)]
    Accounts --> DB
    Transactions --> DB
    Fraud --> DB
    Audit --> DB
    Notifications --> DB

    Shared[shared errors, money helpers, validation] --> API
```

## Package Layout

```text
com.m1banklab
├── identity
├── customers
├── accounts
├── transactions
├── fraud
├── loans
├── audit
├── notifications
└── shared
```

## Boundary Notes

- `customers` owns customer identity/profile data.
- `accounts` owns account state and balance mutation.
- `transactions` owns financial movement records and transfer orchestration.
- `fraud` owns deterministic risk scoring and fraud assessment persistence.
- `audit` owns append-style audit events.
- `notifications` owns outbound notification records.
- `identity` is reserved for future authentication and authorization.
- `loans` is reserved for future origination, underwriting, servicing, and delinquency workflows.
- `shared` contains cross-cutting API errors and money helpers.

## Request Flow

Typical transfer flow:

```mermaid
sequenceDiagram
    participant Client
    participant Controller as TransactionController
    participant Transfer as TransferService
    participant Accounts as AccountRepository
    participant Tx as TransactionService
    participant Fraud as FraudService
    participant Audit as AuditService
    participant DB as PostgreSQL

    Client->>Controller: POST /api/transfers
    Controller->>Transfer: transfer(request)
    Transfer->>Accounts: lock source and target accounts
    Transfer->>Transfer: validate funds and mutate balances
    Transfer->>Tx: recordPosted(...)
    Tx->>DB: insert transaction
    Tx->>Fraud: assess(transaction)
    Fraud->>DB: insert fraud_assessment
    Fraud->>Audit: record FRAUD_ASSESSED
    Tx->>Audit: record TRANSFER_POSTED
    Audit->>DB: insert audit_events
    Controller-->>Client: transaction response
```

## Design Constraints

- Controllers stay thin.
- Services own business logic.
- DTOs define API boundaries.
- JPA entities model persistence state.
- Flyway owns schema changes.
- `BigDecimal` is used for money.
- Account writes use pessimistic locking for balance mutation.
- Every financial action should produce an audit trail.
