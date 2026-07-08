CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    phone VARCHAR(40),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    account_number VARCHAR(32) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    source_account_id UUID REFERENCES accounts(id),
    target_account_id UUID REFERENCES accounts(id),
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    description VARCHAR(240),
    failure_reason VARCHAR(240),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transactions_source_created_at ON transactions(source_account_id, created_at);
CREATE INDEX idx_transactions_target_created_at ON transactions(target_account_id, created_at);

CREATE TABLE fraud_assessments (
    id UUID PRIMARY KEY,
    transaction_id UUID REFERENCES transactions(id),
    risk_level VARCHAR(30) NOT NULL,
    score INTEGER NOT NULL,
    reasons TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID,
    actor VARCHAR(120) NOT NULL,
    details TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES customers(id),
    channel VARCHAR(30) NOT NULL,
    subject VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
