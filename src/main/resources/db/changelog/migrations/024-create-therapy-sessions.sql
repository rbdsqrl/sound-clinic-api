--liquibase formatted sql

--changeset simplehearing:024-create-therapy-sessions
CREATE TABLE therapy_sessions (
    id             UUID PRIMARY KEY,
    org_id         UUID      NOT NULL REFERENCES organisations(id),
    enrollment_id  UUID      NOT NULL REFERENCES enrollments(id),
    patient_id     UUID      NOT NULL REFERENCES patients(id),
    therapist_id   UUID      NOT NULL REFERENCES users(id),
    session_number INT       NOT NULL,
    session_date   DATE      NOT NULL,
    start_time     TIME      NOT NULL,
    end_time       TIME      NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    notes          TEXT,
    completed_by   UUID REFERENCES users(id),
    completed_at   TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

--rollback DROP TABLE therapy_sessions;
