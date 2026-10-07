# m1-banklab technical manual

## The system you are learning

This repository is a Java 21/Spring Boot synthetic banking lab with a browser operations desk. It models invented customers, accounts, deposits, withdrawals, transfers, a transaction journal, deterministic fraud assessments, and audit events in PostgreSQL. It does not connect to a bank, process payments, hold real money, or implement a double-entry ledger. Never enter real customer or financial information.

A **modular monolith** is one application divided into domain packages rather than independently deployed services. A **transaction** is a database unit that commits together or rolls back together; the `transactions` package also uses that word for a stored money-movement record. A **DTO** (data transfer object) defines JSON request/response fields without exposing a persistence entity. **JPA** maps Java objects to relational rows. **Flyway** applies versioned SQL migrations before Hibernate validates the schema.

Start with [AccountService](../src/main/java/com/m1banklab/accounts/AccountService.java), [TransferService](../src/main/java/com/m1banklab/transactions/TransferService.java), and [TransactionService](../src/main/java/com/m1banklab/transactions/TransactionService.java). They show where fictional balances change and which records join the same database transaction. Then read [desk.js](../src/main/resources/static/desk.js) to trace a browser action into those services. Spring serves the desk at `/` from classpath static resources inside the same executable JAR; there is no separate frontend server or application build pipeline.

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

Open `/` for the operations desk, `/swagger-ui.html` for interactive API documentation, or `/v3/api-docs` for OpenAPI JSON on the chosen app port. The `management` configuration alone does not add an Actuator dependency; do not assume `/actuator/health` exists. Node 22 or later is only needed to run JavaScript/browser tests; Java serves the production assets directly.

## Source and data ownership

| Package/file | Role |
| --- | --- |
| [static/index.html](../src/main/resources/static/index.html) | Semantic desk layout, labeled transfer form, live status regions, journal and inspection panels |
| [static/desk.css](../src/main/resources/static/desk.css) | Ivory/oxblood visual system, native controls, visible focus, mobile reflow, reduced-motion rules |
| [static/desk.js](../src/main/resources/static/desk.js) | Same-origin reads, state transitions, safe DOM rendering, transfer submission, evidence joins |
| [static/money.js](../src/main/resources/static/money.js) | Lossless numeric-token parsing and integer-cent arithmetic using BigInt |
| [tests/browser/operations.spec.js](../tests/browser/operations.spec.js) | Browser actions against Spring, direct PostgreSQL assertions, failure recovery, accessibility and screenshots |
| [tests/money.test.js](../tests/money.test.js), [playwright.config.js](../playwright.config.js) | Decimal regressions and real-application browser test lifecycle |
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

## Using and understanding the operations desk

On a new database, Avery Stone has checking `…0001` with 2500.00 and savings `…0002` with 15000.00; Jordan Reed has checking `…0003` with 1000.00. These identities and initial balances come from `V2__seed_data.sql`. The desk knows only the three account UUIDs and two customer UUIDs in that fixture. It fetches names, account numbers, types, statuses, and current balances from Spring; it never substitutes a fixture balance when a request fails. Accounts created through Swagger are outside this desk's selector. This is a deliberate small demonstration surface, not customer/account discovery or authorization.

1. Open `/`. A complete read fills the sample book and the combined balance. Opening balances are not transactions, so a new database has an empty journal.
2. Choose different source and target accounts. Enter `250.00` and an optional fictional note; the **250 units** scenario prepares that amount and a note without sending anything.
3. Submit **Post fictional transfer**. The form and refresh control are disabled while Spring responds. A successful response supplies the transaction UUID; the desk then reads balances and evidence again, clears the amount, and selects that journal entry.
4. Inspect the movement, account names, timestamp, note, risk reasons, and transaction-linked audit events. The journal deduplicates transfers that appear in both the source and target histories. Filtering changes only the displayed account history.
5. Choose **Insufficient funds** to prepare `last source balance + 1.00`. Submit to ask the real server to decide. With no competing changes, expect 422 and unchanged balances/journal. The UI does not fabricate a rejection; a concurrent deposit can change the outcome.
6. Use **Refresh records** after external API activity. It is always a read. There is no background polling, automatic write retry, deposit/withdrawal UI, account creation form, or authentication screen.

The frontend makes these existing calls:

| Trigger | Requests and source of truth |
| --- | --- |
| Initial load, manual refresh, and confirmed/rejected transfer | Three `GET /api/accounts/{id}`, two `GET /api/customers/{id}`, three `GET /api/accounts/{id}/transactions`, one `GET /api/fraud/assessments`, one `GET /api/audit/events` |
| Submit form | One `POST /api/transfers` with source/target UUIDs, decimal amount **as a string**, and optional description |
| Filter or inspect entry | In-memory view of the most recent successful reads; no write |

The ten read requests run in parallel with `cache: no-store`. `refresh()` publishes them together only after all resolve and monetary values parse. These separate HTTP requests are **not a single database snapshot**: another client may write between them. The last-read time and explicit refresh action communicate that limit. A failed read retains old displayed values, marks them stale, and pauses transfers until a complete read succeeds. It does not show invented zeros.

### State and safety of a browser action

`state` in `desk.js` holds the fetched arrays, selected transaction ID, and `busy`, `loading`, `fresh`, `uncertain`, and `storageAvailable` flags. There is one refresh at a time; the form's synchronous `busy` guard prevents overlapping submit events. Native fieldset disabling also covers keyboard and pointer interaction. A successful transfer clears the amount, preventing a completed double click from silently submitting the same amount again.

| State | What the desk does |
| --- | --- |
| Loading or stale | Disable transfer controls; show loading or the failed-read message; retain previous values if available |
| Ready | Enable the seeded-account form after a complete read |
| Posting | Mark `busy` synchronously, persist a pending marker, disable form/refresh, issue exactly one POST |
| Confirmed POSTED response | Remove marker, show the returned UUID, clear inputs, refresh records; a failed follow-up read does not turn success into a rejected transfer |
| Recognized 400/404/409/422 | Display server message/field errors, remove marker, refresh; do not invent a FAILED transfer row |
| Lost, timed-out, malformed, unexpected, or server-error response | Retain marker, label outcome unconfirmed, disable the form, allow read-only refresh and inspection |
| Reload with marker | Restore the unconfirmed warning before loading records; never resubmit |

Before POST, the desk writes `banklab.pending-transfer` to `sessionStorage`. It stores only a pending marker, not account details or credentials. If storage is unavailable, reads still work and transfers remain disabled. This marker protects the warning through reloads of the same tab; closing the tab, clearing storage, other tabs, and direct API clients are outside that mechanism.

Each fetch has a 15-second AbortController timeout. Aborting a browser request **does not cancel or roll back the server transaction**. After uncertainty, the explicit acknowledgement requires a successful read and tells the operator to inspect records before starting a new transfer. A read alone cannot prove an in-flight request has finished, nor uniquely identify a prior request. Acknowledgement clears the form; it does not claim the previous request failed or was deduplicated. Reliable recovery needs a server-side idempotency/status design, described in the exercises below.

### Validation, exact amounts, and rendering

The amount field uses decimal text input and accepts a positive value with 1–17 whole digits and at most two fractional digits. It rejects exponent notation, signs, commas, and fractional cents. The target must differ from the source; notes are limited to 240 characters. Native `reportValidity()` gives keyboard and assistive-technology feedback. The UI deliberately leaves the funds decision to Spring, where `@Valid`, `TransferService`, and the database are authoritative. Client validation is not a security boundary.

API `BigDecimal` values are JSON numeric tokens. Ordinary `JSON.parse` could round a 17-digit monetary value before the UI sees it. `parseApiJson()` tokenizes JSON strings and numbers, preserves quoted strings, and converts numeric tokens to strings before parsing. Consequently other numeric response fields, including risk scores, are also strings in frontend state. `cents()` converts money to `BigInt`, the total is summed in integer cents, and `decimal()` serializes the request without binary floating-point arithmetic. This is an explicit adapter for the existing API shape, not a change to its contract. The decimal tests cover the largest permitted value and a combined balance beyond that value.

All server-provided text enters DOM nodes through `textContent` or native `Option` text; notes never become HTML. Labels, landmarks, a skip link, visible focus, native controls, live status regions, and focus on submission results support keyboard use. The journal uses `aria-pressed` buttons rather than clickable noninteractive rows. The layout stacks on narrow screens and respects reduced motion. Automated axe checks cover selected WCAG rules; they do not replace human assistive-technology review.

`renderDetail()` joins assessments by `transactionId` and audit events by both `aggregateType === TRANSACTION` and `aggregateId`. Both evidence endpoints return only their latest 100 records. Missing matching evidence is labeled as absent from that window, not proof that no record exists. Failed-withdrawal audit events are account-linked, so the desk does not misattribute another withdrawal's event to the selected record. Dates display in the browser's local timezone. Values are labeled **demo units** because the backend has no currency field.

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

[BankingFlowIntegrationTest](../src/test/java/com/m1banklab/BankingFlowIntegrationTest.java) is skipped if Docker is unavailable. A local Maven success with skipped tests is **not** integration evidence. [.github/workflows/ci.yml](../.github/workflows/ci.yml) additionally parses the test report and fails if that class skipped any tests. Reports live under `target/surefire-reports`; the final executable artifact is `target/m1-banklab-0.1.0-SNAPSHOT.jar` and includes the static desk. No standalone frontend build, lint, or static-analysis task is configured.

For browser verification, build the JAR and start a **disposable seeded PostgreSQL database**. The tests post fictional transfers and intentionally leave their journal/evidence rows; they do not reset the database. Never point them at a database you want preserved. With the default Compose settings:

```sh
mvn --batch-mode --no-transfer-progress -DskipTests package
docker compose -p banklab-browser up -d
npm ci
npm test
npx playwright install chromium
npm run test:browser
```

The package step intentionally skips Java tests here because the separate `mvn verify` gate runs them. Playwright starts the packaged application and waits on a seeded account endpoint; locally it can reuse a running service. `BANKLAB_TEST_DATABASE_URL` configures the PostgreSQL connection used by direct assertions and must refer to the **same database** as `SPRING_DATASOURCE_URL`/username/password. `BANKLAB_BASE_URL` configures the browser/service readiness address; set `SERVER_PORT` consistently if using a different port. Defaults are documented in `playwright.config.js` and `operations.spec.js`.

CI has two independent jobs: Maven/JUnit/Testcontainers and a browser job with its own `postgres:16-alpine` service, built Spring JAR, and Chromium. The browser suite uses one worker and no retries. It verifies a real transfer by reading balances and row counts directly with `pg`, and confirms its fraud/audit foreign-key associations. It delays delivery of a real POST response to exercise repeated submits; it loses a response **after a real commit** to test uncertainty across reload. These are network fault injections, not mocked successful writes. Further checks cover real insufficient-funds 422, local validation, stale-read recovery, keyboard inspection, 375/320-pixel reflow, and desktop/mobile axe scans.

The `banking-desk-browser-evidence` CI artifact retains the Playwright HTML report, successful scenario screenshots, and failure screenshots/traces for 14 days. `test-results/` and `playwright-report/` are local outputs, not committed source. Inspect the exact commit's run before claiming it passed. Screenshots in `docs/screenshots` are copied from a successful real-service CI run and have a provenance note.

Practice on a disposable database:

1. Deposit `1.001` using the deposit endpoint. Expect 400, field error, and unchanged account balance.
2. Transfer more than the source holds. Expect 422, both balances unchanged, and no journal record for the rejected transfer.
3. Withdraw more than the source holds. Expect HTTP 200 **with status FAILED**, unchanged balance, and audit/fraud evidence. A client must inspect the body, not only HTTP status.
4. Repeat the successful transfer command twice. Expect two separate transactions and two balance changes. This demonstrates the current lack of idempotency rather than a bug that the UI should hide.
5. Submit a transfer to the same source and target. Expect 422 before any account writes.
6. Stop only your disposable PostgreSQL service, then start the application. Expect a connection/startup failure, not silent in-memory fallback.

For startup authentication errors, compare exported Spring variables with the credentials used when the volume was initialized. For Flyway checksum failures, do not edit applied migrations; add a new migration. For a missing account, verify you are using UUID IDs, not account-number strings. For transaction inconsistencies, inspect both account rows, journal IDs, and audit timestamps within the same database. SQL logs may contain synthetic customer data, so keep real information out of this lab.

For a blank desk, inspect browser console/network errors and confirm `index.html`, `desk.js`, and `money.js` are inside the running JAR; rebuild after static-file edits. A missing fixture account or failed evidence endpoint blocks the complete read and keeps the form disabled. For a posted transfer followed by a failed refresh, use **Refresh records**, not another POST. For an unconfirmed transfer, inspect both balances, matching journal IDs/notes, and server/database logs; the marker is not a correlation key. For a browser test database mismatch, compare its `BANKLAB_TEST_DATABASE_URL` with Spring's datasource and check that the same seed rows exist in both views. A service readiness timeout means the real application never became available; read its startup logs rather than swapping in mocked responses.

## Dependency and security boundaries

[pom.xml](../pom.xml) uses the compatible Spring Boot 3.5.16 patch, which manages Spring Framework 6.2.19, Spring Security 6.5.11, and Tomcat 10.1.55. This updates vulnerable older managed dependencies without switching the framework major line. The public [multipart advisory](https://github.com/advisories/GHSA-cjpg-rgq5-fr37) lists 6.2.19 as a fixed version for its 6.2 branch.

A remaining public [XsltView advisory](https://github.com/advisories/GHSA-pc63-qcmh-9cmg) includes 6.2.19 and lists no fixed 6.2 release at review time. This project does not configure XsltView or accept stylesheet input. That limits the observed exposure, not the advisory's existence. A dependency check is not a guarantee of security. Keep the lab local; reevaluate patched versions before changing deployment scope.

`SecurityConfig` permits every request and disables CSRF. There is no implemented identity module, authorization, tenant isolation, or real actor attribution: audit actors such as `api` are labels. The demo defaults are public values, not production secrets. No change here grants permission to expose this service publicly.

## Extension exercises and solutions

**Add idempotent transfers.** Define a request key and canonical request hash. Add a table with a unique key, request hash, outcome/response reference, and lifecycle timestamps. Claim the key and perform movement in the same transaction. Return the stored response for an identical replay, and 409 for key reuse with a different payload. Solution tests must send concurrent identical requests and assert one balance change; also exercise crashes/rollback and mismatched payloads. An in-memory map or disabled button is insufficient.

**Add a double-entry ledger.** Introduce ledger accounts and immutable postings, with balanced debit/credit totals per journal entry and an explicit currency/precision policy. Reconcile computed postings against balances. Solution tests must prove total debits equal total credits and that opening balances have a corresponding posting, not simply rename the existing `transactions` table.

**Prove atomic downstream failure.** Inject a failure in audit/fraud persistence after account mutation during an integration test. Re-query using a new transaction and assert both balances and all related row counts are unchanged. Mockito's lock-order unit test alone cannot establish SQL rollback behavior.

**Trace a successful UI action.** Set a breakpoint in the submit listener and follow `api('/api/transfers')` into `TransactionController`, `TransferService`, and `TransactionService`. Answer: the browser sends a decimal string, Spring validates/coerces BigDecimal, the service locks both accounts and commits the movement plus evidence, then the browser uses the returned UUID and re-reads state. The UI does not compute authoritative new balances.

**Explain a lost success response.** In the browser regression, find the route that calls `route.fetch()` and then aborts delivery. Answer: PostgreSQL gains one transaction even though the browser says “Outcome unconfirmed.” A reload keeps the warning, and no second POST is sent. A disabled button, session marker, or read-after-timeout cannot establish server idempotency.

**Add paginated evidence inspection.** The current latest-100 endpoints can omit older evidence. Add transaction-specific or cursor-paginated query contracts and preserve transaction-ID correlation. Answer: test an older movement after more than 100 newer assessments; the detail must fetch its own evidence instead of interpreting the global window's absence as no assessment. Include loading/error states and keep account-linked events separate.

**Add a withdrawal form.** Reuse the request/state patterns but inspect the response status. Answer: insufficient withdrawals currently return HTTP 200 with `status: FAILED`, unlike transfer 422. A valid solution renders that failed journal record, preserves balances, and shows matching risk evidence without claiming a posted movement. Verify browser → API → PostgreSQL; do not infer success from `response.ok` alone.

## Interview questions with answers

- **Why BigDecimal?** Decimal arithmetic avoids binary floating-point rounding surprises; scale and validation still need explicit policy.
- **Why lock both accounts consistently?** Opposing transfers otherwise acquire A then B and B then A, producing a deadlock cycle.
- **Why one transaction for fraud and audit?** A committed balance change should not lose its companion evidence because one part failed.
- **Does a fraud assessment prevent the transfer?** No. This implementation observes and records risk inside the posting transaction.
- **What happens on retry?** A new valid request posts again; idempotency is an extension, not an existing feature.
- **Why plain browser JavaScript?** Four small static assets fit the existing Spring packaging and preserve same-origin requests without an extra deployment or runtime. Test dependencies do not ship in the JAR.
- **Why not use `Number` for the combined balance?** The API allows larger decimal values than JavaScript can represent exactly. Numeric-token preservation and integer cents avoid silently rounded display or request values.
- **Does the screen prove a consistent global snapshot?** No. Its parallel GETs publish together in the UI, but each is a separate server request and can observe concurrent changes.
- **What makes this a simulation?** Public fixture values, permissive access, invented money, no external settlement, and incomplete financial/accounting controls.
- **What should be measured before splitting services?** Workload, ownership, failure isolation needs, and operational cost. Package separation alone does not justify distributed transactions.
