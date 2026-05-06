--liquibase formatted sql

--changeset simplehearing:022-create-subscriptions
CREATE TABLE subscriptions (
    id               UUID PRIMARY KEY,
    org_id           UUID NOT NULL REFERENCES organisations(id),
    patient_id       UUID NOT NULL REFERENCES patients(id),
    program_id       UUID NOT NULL REFERENCES programs(id),
    num_sessions     INT NOT NULL,
    per_session_cost DECIMAL(10, 2) NOT NULL,
    discount_percent DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    amount_paid      DECIMAL(10, 2) NOT NULL DEFAULT 0,
    payment_status   VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    payment_notes    TEXT,
    status           VARCHAR(30)    NOT NULL DEFAULT 'ACTIVE',
    created_by       UUID REFERENCES users(id),
    notes            TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

--rollback DROP TABLE subscriptions;
