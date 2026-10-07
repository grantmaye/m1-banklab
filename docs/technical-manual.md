# m1-banklab technical manual

## The system you are learning

This repository is a Java 21/Spring Boot synthetic banking backend. It models invented customers, accounts, deposits, withdrawals, transfers, a transaction journal, deterministic fraud assessments, and audit events in PostgreSQL. It does not connect to a bank, process payments, hold real money, or implement a double-entry ledger. Never enter real customer or financial information.

A **modular monolith** is one application divided into domain packages rather than independently deployed services. A **transaction** is a database unit that commits together or rolls back together; the `transactions` package also uses that word for a stored money-movement record. A **DTO** (data transfer object) defines JSON request/response fields without exposing a persistence entity. **JPA** maps Java objects to relational rows. **Flyway** applies versioned SQL migrations before Hibernate validates the schema.

Start with [AccountService](../src/main/java/com/m1banklab/accounts/AccountService.java), [TransferService](../src/main/java/com/m1banklab/transactions/TransferService.java), and [TransactionService](../src/main/java/com/m1banklab/transactions/TransactionService.java). They show where fictional balances change and which records join the same database transaction. The current interaction surface is REST/Swagger; no separate application frontend is included in this baseline.

## Setup, environment, and a clean database

Prerequisites: Java 21, Maven, Docker with a working daemon, and free local ports. Testcontainers uses disposable PostgreSQL containers with dynamically allocated ports; it does not need the Compose database. Confirm versions with `java -version`, `mvn -version`, and `docker info`.

```sh
mvn --batch-mode --no-transfer-progress verify
cp .env.example .env
docker compose -p banklab-demo up -d
mvn spring-boot:run
```

The default database is localhost:5432 and the app is localhost:8080. Use only an unused Compose project name and port. For an alternate port, pass it consistently to both processes:

```sh
POSTGRES_PORT=55432 docker compose -p banklab-demo up -d
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:55432/m1_banklab SERVER_PORT=18080 mvn spring-boot:run
```

The numbers above are examples: choose free ports. The application can choose a free port automatically with `SERVER_PORT=0`; read its startup log for the assigned value. Stop the app with Ctrl-C and its own Compose project with `docker compose -p banklab-demo down`. The database volume survives. Do not remove volumes to solve a startup issue unless you intentionally want to discard that disposable fixture database.

[application.yml](../src/main/resources/application.yml), [.env.example](../.env.example), and [docker-compose.yml](../docker-compose.yml) define the environment contract:

| Variable | Purpose/default |
| --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Compose initialization values; defaults are public local demo values |
| `POSTGRES_PORT` | Host loopback port mapped to PostgreSQL 5432 |
| `SPRING_DATASOURCE_URL` | JDBC URL, default `jdbc:postgresql://localhost:5432/m1_banklab` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | App database credentials; must agree with initialized database |
| `SERVER_PORT` | App port, default 8080 |
| `SERVER_ADDRESS` | App binding, default 127.0.0.1 |

Compose reads `.env`; a directly launched Maven process does **not** automatically load it. Export the Spring variables in your shell or pass them inline. Changing Compose initialization values does not reinitialize an existing PostgreSQL volume. There are no bank API keys, payment credentials, or live account integrations. Loopback defaults keep this permissive demo off external interfaces; they are not authentication.

Open `/swagger-ui.html` for interactive API documentation or `/v3/api-docs` for OpenAPI JSON on the chosen app port. The `management` configuration alone does not add an Actuator dependency; do not assume `/actuator/health` exists.

## Source and data ownership

| Package/file | Role |
| --- | --- |
| [customers](../src/main/java/com/m1banklab/customers) | Profile request validation, creation, lookup, audit |
| [accounts](../src/main/java/com/m1banklab/accounts) | Account entity, request contracts, pessimistic locks, balance mutation |
| [transactions](../src/main/java/com/m1banklab/transactions) | Transfers, movement journal, account history queries |
| [fraud](../src/main/java/com/m1banklab/fraud) | Explainable scoring and stored assessments |
| [audit](../src/main/java/com/m1banklab/audit) | Append-style audit records and latest-event endpoint |
| [notifications](../src/main/java/com/m1banklab/notifications) | Stored system notification records; no email/SMS delivery |
| [shared](../src/main/java/com/m1banklab/shared) | Money normalization, errors and exception translation |
| [SecurityConfig](../src/main/java/com/m1banklab/config/SecurityConfig.java) | Permit-all configuration; no effective account ownership enforcement |
| [V1__init.sql](../src/main/resources/db/migration/V1__init.sql) | Six-table schema and query indexes |
| [V2__seed_data.sql](../src/main/resources/db/migration/V2__seed_data.sql) | Two fictional customers, three accounts, one seed audit event |
| [identity](../src/main/java/com/m1banklab/identity/README.md), [loans](../src/main/java/com/m1banklab/loans/README.md) | Future module notes, not implemented features |

Schema summary:

| Table | Relationships and meaningful columns |
| --- | --- |
| `customers` | UUID PK; unique email; names, phone, status, timestamps |
| `accounts` | Customer FK; unique generated account number; type/status; `NUMERIC(19,2)` balance |
| `transactions` | Nullable source/target account FKs; type, status, amount, description/failure reason, creation time |
| `fraud_assessments` | Transaction FK; risk level, integer score, textual reasons, timestamp |
| `audit_events` | Event/aggregate types, optional aggregate UUID, actor, details, timestamp |
| `notifications` | Optional customer FK; channel, subject, body, status, timestamp |

UUID means universally unique identifier. Java constructors create IDs; account numbers are derived from random UUID bits, with a database uniqueness constraint. They are simulation identifiers, not routable bank account details. There is no retry for a rare account-number collision.

Amounts use decimal `BigDecimal`, not binary floating point. Public money requests allow at most 17 integer digits and two fractional digits, matching `NUMERIC(19,2)`. Opening balance may be zero; movement amounts must be at least 0.01. `Money.normalize` still rounds with HALF_UP for internal callers; request validation prevents silent fractional-cent rounding at the HTTP boundary. Direct service/entity calls must honor the same contract. Aggregate balance overflow can still become a database conflict; there is no currency model or multi-currency arithmetic.

## Walk through the actual API

With a newly seeded disposable database, set the base URL for your chosen app port:

```sh
BASE=http://127.0.0.1:8080
SOURCE=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
TARGET=cccccccc-cccc-cccc-cccc-cccccccccccc
curl -fsS "$BASE/api/accounts/$SOURCE"
curl -fsS "$BASE/api/accounts/$TARGET"
curl -fsS -X POST "$BASE/api/transfers" -H 'Content-Type: application/json' \
  -d '{"sourceAccountId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","targetAccountId":"cccccccc-cccc-cccc-cccc-cccccccccccc","amount":"250.00","description":"synthetic demo transfer"}'
curl -fsS "$BASE/api/accounts/$SOURCE/transactions"
curl -fsS "$BASE/api/fraud/assessments"
curl -fsS "$BASE/api/audit/events"
```

Initial source/target balances are 2500.00 and 1000.00. After one successful transfer they are 2250.00 and 1250.00. Query accounts again to prove it. The journal includes one POSTED TRANSFER, fraud has an assessment, and audit includes FRAUD_ASSESSED and TRANSFER_POSTED. Opening balances are stored directly; they do not create movement journal entries.

| Method/path | Input and response behavior |
| --- | --- |
| `POST /api/customers` | firstName, lastName, email, optional phone; 201 profile |
| `GET /api/customers/{id}` | 200 profile or 404 |
| `POST /api/accounts` | customerId, type CHECKING/SAVINGS, openingBalance; 201 account |
| `GET /api/accounts/{id}` | 200 account or 404 |
| `POST /api/accounts/{id}/deposit` | amount, optional description; 200 POSTED transaction |
| `POST /api/accounts/{id}/withdraw` | Same input; 200 with POSTED or FAILED status |
| `POST /api/transfers` | sourceAccountId, targetAccountId, amount, optional description; 201 or business-rule error |
| `GET /api/accounts/{id}/transactions` | Unpaginated journal list, newest first; unknown account currently returns an empty list |
| `GET /api/fraud/assessments` | Latest 100 assessments |
| `GET /api/audit/events` | Latest 100 audit events |

Read [request/response records](../src/main/java/com/m1banklab/accounts) and [ApiError](../src/main/java/com/m1banklab/shared/ApiError.java) for exact shapes. Validation errors use 400 with `fieldErrors`; missing entities use 404; business rules use 422; database integrity conflicts use 409. Invalid JSON/type conversion uses Spring's own handling rather than this project's `MethodArgumentNotValidException` mapping, so not every error body has the same shape.

**Duplicate requests are not protected.** There is no idempotency key. Repeating a valid transfer debits again. A timeout is not proof that nothing committed; clients should inspect state before deciding what to do. Do not demonstrate automatic retry as safe.

## Transaction boundaries and concurrency

```mermaid
sequenceDiagram
  participant API as TransactionController
  participant Transfer as TransferService
  participant DB as PostgreSQL
  participant Journal as TransactionService
  participant Fraud as FraudService
  API->>Transfer: validated transfer DTO
  Transfer->>DB: lock lower UUID, then higher UUID
  Transfer->>Transfer: check funds; debit and credit
  Transfer->>Journal: recordPosted
  Journal->>DB: transaction row
  Journal->>Fraud: assess
  Fraud->>DB: assessment + audit + possible notification
  Journal->>DB: movement audit
  Transfer->>DB: commit everything
```

`@Transactional` starts the unit of work around account and transfer services. `PESSIMISTIC_WRITE` locks account rows until the transaction ends, preventing overlapping balance mutations from reading a stale balance. Every transfer acquires both locks in the same UUID order, even for A→B and B→A. This removes the simple opposing-transfer lock cycle; it does not claim all possible database deadlocks are impossible.

`TransactionService`, `FraudService.assess`, `AuditService.record`, and notification recording require an existing transaction with `Propagation.MANDATORY`. If downstream persistence throws a runtime exception, the outer work rolls back. JPA dirty checking writes modified managed account balances at flush/commit; an explicit account `save` after every mutation is unnecessary.

A failed withdrawal is deliberately recorded with status FAILED and assessed for risk, then returned with HTTP 200. Insufficient-funds transfers throw 422 before mutating balances and do not create a failed transfer journal entry. Same-account transfers are rejected. These are implemented differences, not interchangeable error handling.

The schema is a balance-plus-journal model. It lacks debit/credit posting pairs, currency, reconciliation, immutable database-enforced audit storage, account ownership checks, and idempotent command processing. Do not call it a double-entry ledger.

## Explainable fraud rules

[FraudRules](../src/main/java/com/m1banklab/fraud/FraudRules.java) adds scores:

- Amount strictly above 10,000: +80; otherwise strictly above 5,000: +45.
- More than six recent transactions: +55; otherwise more than three: +35.
- Transfer to a previously unseen payee: +35.
- FAILED transaction: +20.

Scores ≥70 are HIGH; ≥35 are MEDIUM; lower scores are LOW. The ten-minute count queries movements involving the source account, includes the current saved transaction after JPA flush, and includes failed movements. Deposits have no source account and do not use that velocity count. A new payee check excludes the current transaction ID. Exactly 5,000 does not meet the amount rule; exactly 10,000 receives +45, not +80.

Scoring happens after balance mutation inside the transaction. It records a decision; it does **not** block high-risk transfers. HIGH creates a SYSTEM notification row, not an external alert. This is a transparent teaching rule engine, not a validated financial fraud model.

## Verification, failure labs, and debugging

Run `mvn --batch-mode --no-transfer-progress verify`. Unit tests cover rounding/positivity, fraud thresholds, and both directions of transfer lock acquisition. PostgreSQL integration tests cover customer/account creation, deposit, failed withdrawal, transfer, journal/fraud/audit records, fractional-cent and oversized amount rejection, and insufficient-transfer balance preservation.

[BankingFlowIntegrationTest](../src/test/java/com/m1banklab/BankingFlowIntegrationTest.java) is skipped if Docker is unavailable. A local Maven success with skipped tests is **not** integration evidence. [.github/workflows/ci.yml](../.github/workflows/ci.yml) additionally parses the test report and fails if that class skipped any tests. Reports live under `target/surefire-reports`; the final executable artifact is `target/m1-banklab-0.1.0-SNAPSHOT.jar`. No separate frontend, lint or static-analysis task is configured in this baseline.

Practice on a disposable database:

1. Deposit `1.001` using the deposit endpoint. Expect 400, field error, and unchanged account balance.
2. Transfer more than the source holds. Expect 422, both balances unchanged, and no journal record for the rejected transfer.
3. Withdraw more than the source holds. Expect HTTP 200 **with status FAILED**, unchanged balance, and audit/fraud evidence. A client must inspect the body, not only HTTP status.
4. Repeat the successful transfer command twice. Expect two separate transactions and two balance changes. This demonstrates the current lack of idempotency rather than a bug that the UI should hide.
5. Submit a transfer to the same source and target. Expect 422 before any account writes.
6. Stop only your disposable PostgreSQL service, then start the application. Expect a connection/startup failure, not silent in-memory fallback.

For startup authentication errors, compare exported Spring variables with the credentials used when the volume was initialized. For Flyway checksum failures, do not edit applied migrations; add a new migration. For a missing account, verify you are using UUID IDs, not account-number strings. For transaction inconsistencies, inspect both account rows, journal IDs, and audit timestamps within the same database. SQL logs may contain synthetic customer data, so keep real information out of this lab.

## Dependency and security boundaries

[pom.xml](../pom.xml) uses the compatible Spring Boot 3.5.16 patch, which manages Spring Framework 6.2.19, Spring Security 6.5.11, and Tomcat 10.1.55. This updates vulnerable older managed dependencies without switching the framework major line. The public [multipart advisory](https://github.com/advisories/GHSA-cjpg-rgq5-fr37) lists 6.2.19 as a fixed version for its 6.2 branch.

A remaining public [XsltView advisory](https://github.com/advisories/GHSA-pc63-qcmh-9cmg) includes 6.2.19 and lists no fixed 6.2 release at review time. This project does not configure XsltView or accept stylesheet input. That limits the observed exposure, not the advisory's existence. A dependency check is not a guarantee of security. Keep the lab local; reevaluate patched versions before changing deployment scope.

`SecurityConfig` permits every request and disables CSRF. There is no implemented identity module, authorization, tenant isolation, or real actor attribution: audit actors such as `api` are labels. The demo defaults are public values, not production secrets. No change here grants permission to expose this service publicly.

## Extension exercises and solutions

**Add idempotent transfers.** Define a request key and canonical request hash. Add a table with a unique key, request hash, outcome/response reference, and lifecycle timestamps. Claim the key and perform movement in the same transaction. Return the stored response for an identical replay, and 409 for key reuse with a different payload. Solution tests must send concurrent identical requests and assert one balance change; also exercise crashes/rollback and mismatched payloads. An in-memory map or disabled button is insufficient.

**Add a double-entry ledger.** Introduce ledger accounts and immutable postings, with balanced debit/credit totals per journal entry and an explicit currency/precision policy. Reconcile computed postings against balances. Solution tests must prove total debits equal total credits and that opening balances have a corresponding posting, not simply rename the existing `transactions` table.

**Prove atomic downstream failure.** Inject a failure in audit/fraud persistence after account mutation during an integration test. Re-query using a new transaction and assert both balances and all related row counts are unchanged. Mockito's lock-order unit test alone cannot establish SQL rollback behavior.

**Build an operations UI.** Read actual APIs and display account balances, validated transfer inputs, the returned journal record, and relevant audit/fraud entries. Clearly state synthetic mode and the lack of replay protection. Solution checks should drive a real browser against Spring plus disposable PostgreSQL and confirm the database, not only a mocked success toast. Do not invent a real payments connection.

## Interview questions with answers

- **Why BigDecimal?** Decimal arithmetic avoids binary floating-point rounding surprises; scale and validation still need explicit policy.
- **Why lock both accounts consistently?** Opposing transfers otherwise acquire A then B and B then A, producing a deadlock cycle.
- **Why one transaction for fraud and audit?** A committed balance change should not lose its companion evidence because one part failed.
- **Does a fraud assessment prevent the transfer?** No. This implementation observes and records risk inside the posting transaction.
- **What happens on retry?** A new valid request posts again; idempotency is an extension, not an existing feature.
- **What makes this a simulation?** Public fixture values, permissive access, invented money, no external settlement, and incomplete financial/accounting controls.
- **What should be measured before splitting services?** Workload, ownership, failure isolation needs, and operational cost. Package separation alone does not justify distributed transactions.
