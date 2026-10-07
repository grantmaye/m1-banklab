# m1-banklab: the product story

## Why this repository is useful

A banking-themed CRUD demo can show fields and buttons without showing what happens when two requests compete for money, a request contains fractional cents, or a downstream record fails. `m1-banklab` makes those backend decisions visible through a compact Java/Spring application and a real relational database.

The audience is learners practicing Java backend engineering, interviewers reviewing transaction reasoning, and developers exploring how domain packages, persistence, validation, and audit records fit together. It is a synthetic systems lab. The repository does not establish a real bank, customers, revenue, regulatory approval, employer origin, or production scale.

## A fictional demonstration scenario

Imagine a fictional training team, Juniper Workshop, learning backend correctness. Two invented customers hold fictional checking and savings balances. A developer wants to demonstrate a transfer and then explain why merely returning “success” is insufficient evidence.

Before the lab, the team discusses a diagram and hand-waves database behavior. With the lab, they use Swagger or curl to transfer 250.00, query both accounts, inspect the transaction journal, and find corresponding fraud and audit records. They then request more than the source holds, observe 422, and verify that both balances remain unchanged. A failed withdrawal intentionally behaves differently: it returns a FAILED journal record with HTTP 200.

This is an illustrative scenario, not a claim of actual adoption. Its value is the ability to reproduce and discuss concrete decisions.

## The workflow and evidence

| Question | What a reviewer can inspect |
| --- | --- |
| Did both balances change consistently? | Account queries, locked services, database integration tests |
| Can opposing transfers acquire locks safely? | Consistent UUID lock order and regression test for both directions |
| Are fractional cents silently accepted? | HTTP validation rejecting values with more than two decimals |
| Why was a movement assessed as high risk? | Stored score and deterministic textual reasons |
| What else happened with a movement? | Transaction journal, audit entries, optional stored system notification |
| Can a retry duplicate the effect? | Yes: no idempotency feature exists, and the manual demonstrates that limit |

Sources: [AccountService](../src/main/java/com/m1banklab/accounts/AccountService.java), [TransferService](../src/main/java/com/m1banklab/transactions/TransferService.java), [FraudRules](../src/main/java/com/m1banklab/fraud/FraudRules.java), and [integration tests](../src/test/java/com/m1banklab/BankingFlowIntegrationTest.java).

## What it is not ready to do

The system has no real account or payment connections, no effective authentication/authorization, no idempotency keys, no currency model, and no double-entry ledger or reconciliation. Fraud scoring records risk after posting and does not block money movement. Notifications are database rows, not sent messages. Audit records are append-style application records, not tamper-proof storage. The current baseline exposes APIs and Swagger, without a separate operations UI.

These limits are part of the learning story. Calling the transaction journal a production ledger, or claiming that retrying is safe, would conceal the most useful next engineering problems. Keep the application on localhost and use only invented data.

## 60–90 second demo narration

“m1-banklab is a synthetic banking systems lab built with Java, Spring Boot, and PostgreSQL. These customers and balances are fictional, and no real money moves. I start with two account responses, then request a 250-unit transfer through the API.

“Here are the updated balances: one decreased and the other increased by the same amount. The transaction journal contains the posted transfer. The fraud assessment explains which rules matched, and the audit events show the posting and assessment. Those records participate in the same database transaction.

“Now I attempt a transfer larger than the source balance. The API rejects it, and both balances remain unchanged. I also submit a fractional-cent amount, which validation rejects rather than silently rounding. The locking code acquires account locks in a consistent order, including transfers in opposite directions.

“This is a backend learning tool, not a real bank or a double-entry accounting system. Repeating a successful request currently posts again because idempotency is not implemented. That limitation gives us a concrete next design exercise: reliable command deduplication backed by database constraints and integration tests.”

The [technical manual](technical-manual.md) contains exact commands, schema and endpoint maps, transaction reasoning, failure labs, and extension solutions.
