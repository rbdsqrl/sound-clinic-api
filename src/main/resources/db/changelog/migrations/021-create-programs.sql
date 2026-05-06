--liquibase formatted sql

--changeset simplehearing:021-create-programs
CREATE TABLE programs (
    id               UUID PRIMARY KEY,
    org_id           UUID NOT NULL REFERENCES organisations(id),
    name             VARCHAR(255) NOT NULL,
    per_session_cost DECIMAL(10, 2) NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_by       UUID REFERENCES users(id),
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

--rollback DROP TABLE programs;
