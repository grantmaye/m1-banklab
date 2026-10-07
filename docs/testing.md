# Testing Strategy

`m1-banklab` tests domain rules, API flows, and the operations desk against real PostgreSQL persistence.

## Test Types

### Unit Tests

Unit tests cover behavior that does not require Spring or PostgreSQL.

Current examples:

- `FraudRulesTest`
- `MoneyTest`
- `TransferServiceTest` (both transfer lock directions)

These tests should stay fast and focused.

### Integration Tests

`BankingFlowIntegrationTest` starts a Spring Boot test context and uses Testcontainers PostgreSQL.

It verifies:

- Customer creation.
- Account creation.
- Deposit.
- Failed withdrawal due to insufficient funds.
- Transfer.
- Account transaction listing.
- Fraud assessment persistence.
- Audit event persistence.

## Local Behavior

The integration test uses:

```java
@Testcontainers(disabledWithoutDocker = true)
```

That means local runs can still pass if Docker Desktop is not available or not discoverable. On GitHub Actions, Docker is available, so the integration test runs fully.

## CI Behavior

GitHub Actions runs:

```sh
mvn --batch-mode --no-transfer-progress verify
```

CI also parses the PostgreSQL integration test report and requires zero skipped tests. Inspect the run associated with the commit under review rather than relying on a historical test count. The suite currently includes precision/size validation and insufficient-transfer balance preservation in addition to the original banking flow.

Local `mvn verify` can succeed with integration tests skipped when Docker is absent; inspect `target/surefire-reports` before claiming database validation. The package build produces an executable Spring Boot JAR.

## Browser → Spring → PostgreSQL

The browser CI job has its own disposable PostgreSQL 16 service and starts the packaged JAR. It runs two exact-decimal Node tests and seven Playwright scenarios with one worker and no retries. It does not replace the Java test job.

- Submit a transfer, delay its actual server response, and attempt repeated submissions. Independently query PostgreSQL for one movement, exact balance changes, fraud assessment, and audit events.
- Submit more than the last read balance; require a real 422 and unchanged balances/journal.
- Reject fractional cents and same-account input before sending a POST.
- Commit a real transfer and deliberately lose its response. Require an unconfirmed outcome, persisted warning across reload, and no automatic replay.
- Fail a GET; retain clearly stale values and disable transfers until a complete refresh succeeds.
- Confirm a transfer, fail its follow-up read, and preserve the confirmed-success message without reposting.
- Inspect/filter with the keyboard on mobile, check 375/320-pixel reflow, and scan desktop/mobile with axe.

Fault injection controls response delivery around real requests. The committed browser suite does not mock successful balances or POST outcomes. The direct database assertions use `pg` and must connect to the same database as Spring. Automated accessibility scans are useful checks, not a complete accessibility certification.

For local execution, use an isolated disposable database because tests leave fictional movements behind:

```sh
mvn --batch-mode --no-transfer-progress -DskipTests package
docker compose -p banklab-browser up -d
npm ci
npm test
npx playwright install chromium
npm run test:browser
```

Playwright starts the JAR or reuses an existing local service. The default browser URL is `http://127.0.0.1:8080`; the database is `postgresql://m1_banklab:m1_banklab@127.0.0.1:5432/m1_banklab`. For alternate ports, set `BANKLAB_BASE_URL` and `SERVER_PORT` together, and set `BANKLAB_TEST_DATABASE_URL` plus `SPRING_DATASOURCE_URL`/username/password to the same disposable database. Java must be on PATH.

Reports are in `playwright-report/` and `test-results/`. CI uploads `banking-desk-browser-evidence` for 14 days, including successful desktop, mobile, rejected-transfer, and unconfirmed-transfer screenshots, plus failure traces/screenshots if a case fails. Check the exact commit's run and the skip counts before describing it as verified. Published screenshot provenance is in [screenshots/README.md](screenshots/README.md).

## Future Test Coverage

Recommended next tests:

- Concurrent withdrawals from the same account.
- Transfer rollback if fraud or audit persistence fails.
- Validation errors for malformed request bodies.
- Idempotency-key replay behavior.
- Double-entry ledger balance assertions.
- Role-based authorization once identity is implemented.
