INSERT INTO customers (id, first_name, last_name, email, phone, status, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Avery', 'Stone', 'avery.stone@example.com', '+1-336-555-0101', 'ACTIVE', now(), now()),
    ('22222222-2222-2222-2222-222222222222', 'Jordan', 'Reed', 'jordan.reed@example.com', '+1-336-555-0102', 'ACTIVE', now(), now());

INSERT INTO accounts (id, customer_id, account_number, type, status, balance, created_at, updated_at)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', '100000000001', 'CHECKING', 'OPEN', 2500.00, now(), now()),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', '100000000002', 'SAVINGS', 'OPEN', 15000.00, now(), now()),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', '100000000003', 'CHECKING', 'OPEN', 1000.00, now(), now());

INSERT INTO audit_events (id, event_type, aggregate_type, aggregate_id, actor, details, created_at)
VALUES
    ('99999999-9999-9999-9999-999999999999', 'SEED_DATA_LOADED', 'SYSTEM', NULL, 'system', 'Sample customers and accounts loaded by Flyway.', now());
