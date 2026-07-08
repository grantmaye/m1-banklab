# Banking Domain Notes

This project is intentionally focused on backend systems concerns that matter in banking and fintech environments.

## Money Movement

Money movement is modeled through account balance updates and transaction records. The current MVP updates balances directly and records a transaction for each deposit, withdrawal, or transfer.

The next major domain improvement is a double-entry ledger:

- Every movement creates journal entries.
- Debits and credits must balance.
- Account balances can be derived from ledger entries.
- Reversals are separate transactions, not destructive edits.

## Auditability

Financial systems need traceability. This MVP records audit events for important actions such as:

- Customer creation.
- Account creation.
- Posted deposits.
- Posted withdrawals.
- Posted transfers.
- Fraud assessments.
- Failed withdrawals due to insufficient funds.

The audit log is intentionally separate from transaction records. Transactions explain money movement; audit events explain system behavior.

## Fraud Scoring

The MVP fraud engine is deterministic and explainable:

- Amount over `5000` = medium risk.
- Amount over `10000` = high risk.
- More than 3 transactions in 10 minutes = elevated risk.
- More than 6 transactions in 10 minutes = high velocity risk.
- Transfer to a new payee = medium risk.
- Failed transaction adds risk context.

Each assessment stores:

- Transaction ID.
- Risk level.
- Numeric score.
- Human-readable reasons.

The intent is to show a fraud system shape that can later evolve into configurable rules, analyst review queues, and case management.

## Consistency

The MVP uses service-layer transactions and pessimistic account locking for balance mutation. This keeps concurrent deposits, withdrawals, and transfers from casually overwriting account balances.

Future work should add:

- Idempotency keys.
- Ledger journal tables.
- Transaction limits.
- Reversal flows.
- Outbox events.

## Modularity

The codebase is a modular monolith, not a microservice system. This keeps development and deployment simple while still making boundaries explicit.

Good future service boundaries:

- Identity service.
- Customer service.
- Account/ledger service.
- Fraud service.
- Notification service.
- Loan service.

The current package structure is designed to make those boundaries visible before adding network complexity.

## What This Is Not

This is not production banking software. It does not hold real funds, satisfy regulatory requirements, or replace bank-grade controls.

It is a portfolio and learning project that demonstrates backend engineering patterns used in banking-adjacent systems.
