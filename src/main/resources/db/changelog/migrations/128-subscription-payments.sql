--liquibase formatted sql

--changeset simplehearing:128-subscription-payments
-- One row per payment taken against a plan, so a renewal's extra payment (and every later top-up) shows up as
-- its own dated entry. subscriptions.amount_paid stays the running total; a negative amount is a correction.
CREATE TABLE IF NOT EXISTS subscription_payments (
    id               UUID          NOT NULL PRIMARY KEY,
    org_id           UUID          NOT NULL,
    subscription_id  UUID          NOT NULL REFERENCES subscriptions (id) ON DELETE CASCADE,
    amount           NUMERIC(12,2) NOT NULL,
    paid_on          DATE          NOT NULL,
    mode             VARCHAR(30),
    note             TEXT,
    recorded_by      UUID,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_subscription_payments_sub ON subscription_payments (subscription_id, paid_on DESC, created_at DESC);

--rollback DROP TABLE IF EXISTS subscription_payments;
