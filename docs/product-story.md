# m1-banklab: the product story

## Why this repository is useful

A banking-themed CRUD demo can show fields and buttons without showing what happens when two requests compete for money, a request contains fractional cents, or a downstream record fails. `m1-banklab` makes those backend decisions visible through a compact Java/Spring application, a real relational database, and a browser operations desk that follows a movement from request to stored evidence.

The audience is learners practicing Java backend engineering, interviewers reviewing transaction reasoning, and developers exploring how domain packages, persistence, validation, and audit records fit together. It is a synthetic systems lab. The repository does not establish a real bank, customers, revenue, regulatory approval, employer origin, or production scale.

## A fictional demonstration scenario

Imagine a fictional training team, Juniper Workshop, learning backend correctness. Two invented customers hold fictional checking and savings balances. A developer wants to demonstrate a transfer and then explain why merely returning “success” is insufficient evidence.

Before the lab, the team discusses a diagram and hand-waves database behavior. With the lab, they open an ivory-and-oxblood operations desk showing Avery Stone's checking and savings accounts alongside Jordan Reed's checking account. They transfer 250.00 fictional units, see both balances read back from the server, and select the journal entry to inspect its fraud assessment and audit records. They then prepare an insufficient-funds scenario, submit it, observe the real 422 response, and verify that both balances remain unchanged.

The visual character follows a small operations book: quiet paper tones, compact account rows, serif headings, and a clear transaction inspection panel. There are no invented charts, account-growth claims, or real bank branding. The interface creates a useful demonstration path without competing with the backend lessons.

For a second lesson, the trainer uses the browser regression that deliberately loses a response after Spring commits. The database contains the movement while the desk shows “Outcome unconfirmed.” The team learns why a disabled button prevents an accidental repeat in one tab but cannot guarantee exactly-once processing. Swagger remains available for the wider API surface; a failed withdrawal intentionally differs from a rejected transfer by returning a FAILED journal record with HTTP 200.

This is an illustrative scenario, not a claim of actual adoption. Its value is the ability to reproduce and discuss concrete decisions.

## The workflow and evidence

| Question | What a reviewer can inspect |
| --- | --- |
| Can someone understand the backend without crafting curl requests first? | Seeded account book, transfer form, journal filter, and transaction inspection in the same Spring app |
| Did both balances change consistently? | Account queries, locked services, database integration tests |
| Can opposing transfers acquire locks safely? | Consistent UUID lock order and regression test for both directions |
| Are fractional cents silently accepted? | HTTP validation rejecting values with more than two decimals |
| Why was a movement assessed as high risk? | Stored score and deterministic textual reasons |
| What else happened with a movement? | Transaction journal, audit entries, optional stored system notification |
| Can a retry duplicate the effect? | Yes: no idempotency feature exists, and the manual demonstrates that limit |
| Did the browser actually cause a database change? | CI runs Chromium against the packaged Spring app and PostgreSQL; tests independently query balances, transaction rows, fraud assessments, and audit events |

Sources: [operations desk](../src/main/resources/static/desk.js), [AccountService](../src/main/java/com/m1banklab/accounts/AccountService.java), [TransferService](../src/main/java/com/m1banklab/transactions/TransferService.java), [FraudRules](../src/main/java/com/m1banklab/fraud/FraudRules.java), [integration tests](../src/test/java/com/m1banklab/BankingFlowIntegrationTest.java), and [browser/database tests](../tests/browser/operations.spec.js).

The learner benefits from a visible path into Java transaction boundaries and decimal handling. The reviewer benefits from being able to ask “where is the evidence?” and open both the UI and its source. A developer extending the lab benefits from explicit failure examples and testable next steps. These are educational benefits, not measured financial outcomes or claims that an organization uses this software.

## What it is not ready to do

The system has no real account or payment connections, no effective authentication/authorization, no idempotency keys, no currency model, and no double-entry ledger or reconciliation. Fraud scoring records risk after posting and does not block money movement. Notifications are database rows, not sent messages. Audit records are append-style application records, not tamper-proof storage. The desk offers the three seeded accounts and transfers; it is not a customer portal or an account-management product. Evidence endpoints show their latest 100 records, and separate account reads do not form an atomic global snapshot.

These limits are part of the learning story. Calling the transaction journal a production ledger, or claiming that retrying is safe, would conceal the most useful next engineering problems. Keep the application on localhost and use only invented data.

## 60–90 second demo narration

“m1-banklab is a synthetic banking systems lab built with Java, Spring Boot, and PostgreSQL. These customers and balances are fictional, and no real money moves. I open the operations desk, choose Avery's checking account and Jordan's checking account, and submit a 250-unit transfer.

“Here are the balances read back from the server: one decreased and the other increased by the same amount. I select the posted transfer in the journal. Its transaction ID connects the risk reasons to the audit events. Those stored records participate in the same database transaction.

“The insufficient-funds button prepares a larger amount without sending it. I submit and the real API rejects it; both balances stay unchanged. The form also rejects fractional cents, and the backend independently enforces that rule. In the source, the transfer locks accounts in a consistent order, including transfers in opposite directions.

“This is a backend learning tool, not a real bank or a double-entry accounting system. Repeating a successful request currently posts again because idempotency is not implemented. That limitation gives us a concrete next design exercise: reliable command deduplication backed by database constraints and integration tests.”

The [technical manual](technical-manual.md) contains exact commands, schema and endpoint maps, transaction reasoning, failure labs, and extension solutions.
