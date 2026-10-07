# Testing Strategy

`m1-banklab` uses a small but meaningful test suite that covers deterministic domain rules and a full API flow.

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
mvn test
```

CI also parses the PostgreSQL integration test report and requires zero skipped tests. Inspect the run associated with the commit under review rather than relying on a historical test count. The suite currently includes precision/size validation and insufficient-transfer balance preservation in addition to the original banking flow.

Local `mvn verify` can succeed with integration tests skipped when Docker is absent; inspect `target/surefire-reports` before claiming database validation. The package build produces an executable Spring Boot JAR.

## Future Test Coverage

Recommended next tests:

- Concurrent withdrawals from the same account.
- Transfer rollback if fraud or audit persistence fails.
- Validation errors for malformed request bodies.
- Idempotency-key replay behavior.
- Double-entry ledger balance assertions.
- Role-based authorization once identity is implemented.
