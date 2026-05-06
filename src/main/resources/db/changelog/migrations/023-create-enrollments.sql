--liquibase formatted sql

--changeset simplehearing:023-create-enrollments
CREATE TABLE enrollments (
    id                       UUID PRIMARY KEY,
    org_id                   UUID NOT NULL REFERENCES organisations(id),
    subscription_id          UUID NOT NULL REFERENCES subscriptions(id),
    patient_id               UUID NOT NULL REFERENCES patients(id),
    therapist_id             UUID NOT NULL REFERENCES users(id),
    session_duration_minutes INT         NOT NULL,
    start_date               DATE        NOT NULL,
    day_of_week              VARCHAR(10) NOT NULL,
    start_time               TIME        NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    sessions_completed       INT         NOT NULL DEFAULT 0,
    created_by               UUID REFERENCES users(id),
    created_at               TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP   NOT NULL DEFAULT now()
);

--rollback DROP TABLE enrollments;
