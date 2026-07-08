# Roadmap

This roadmap is organized around portfolio value and banking-system realism. Items are intentionally scoped as issues that can be implemented independently.

## Phase 1: Ledger Correctness

- Add idempotency keys for deposits, withdrawals, and transfers.
- Replace balance-only accounting with double-entry ledger journal entries.
- Add account holds and pending transaction states.
- Add transaction reversal flow with audit history.
- Add daily withdrawal and transfer limits.

## Phase 2: Identity And Authorization

- Add authenticated users.
- Add role-based access control for teller, fraud analyst, auditor, and admin roles.
- Add customer-level authorization checks.
- Add service-account style authentication for internal jobs.
- Add audit events for authentication and privileged access.

## Phase 3: Payments And Batch Processing

- Add ACH-style payment batches.
- Add payment status lifecycle: pending, submitted, settled, returned.
- Add cut-off windows and settlement dates.
- Add retry and return-code handling.
- Add CSV import for batch payment files.

## Phase 4: Fraud And Case Management

- Add configurable fraud rules stored in the database.
- Add fraud cases for medium/high risk transactions.
- Add analyst review status and notes.
- Add allowlist/blocklist support for payees.
- Add velocity rules by customer, account, and payee.

## Phase 5: Event-Driven Architecture

- Add an outbox table.
- Publish domain events for transaction posted, fraud assessed, and notification queued.
- Add async notification worker.
- Add retry and dead-letter behavior.
- Add event replay notes and operational docs.

## Phase 6: Loans

- Add loan applications.
- Add underwriting decision model.
- Add amortization schedules.
- Add payment posting.
- Add delinquency and collections state.

## Phase 7: Observability And Operations

- Add structured JSON logs.
- Add correlation IDs.
- Add Micrometer metrics.
- Add OpenTelemetry tracing.
- Add health/readiness endpoints tuned for deployment.

## Phase 8: API And Developer Experience

- Add OpenAPI-generated client examples.
- Add Postman collection.
- Add architecture decision records.
- Add richer sample data.
- Add screenshots to `docs/screenshots`.

## Good First Issues

- Add `GET /api/customers/{id}/accounts`.
- Add `GET /api/notifications`.
- Add validation that account opening balance must be normalized to two decimals.
- Add error response examples to the README.
- Add a Postman collection export.
