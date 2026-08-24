--liquibase formatted sql

--changeset simplehearing:062-create-enrollment-concerns
-- A parent-raised concern about an ongoing program, independent of the review-meeting cadence.
CREATE TABLE IF NOT EXISTS enrollment_concerns (
    id               UUID         PRIMARY KEY,
    org_id           UUID         NOT NULL,
    enrollment_id    UUID         NOT NULL,
    patient_id       UUID         NOT NULL,
    therapist_id     UUID         NOT NULL,
    raised_by        UUID         NOT NULL,
    raised_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    description      TEXT         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    acknowledged_by  UUID,
    acknowledged_at  TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    resolved_by      UUID,
    resolved_at      TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_enrollment_concerns_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_concerns_patient    FOREIGN KEY (patient_id)    REFERENCES patients (id)    ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_concerns_therapist  FOREIGN KEY (therapist_id)  REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_enrollment_concerns_org         ON enrollment_concerns (org_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_concerns_enrollment  ON enrollment_concerns (enrollment_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_concerns_patient     ON enrollment_concerns (patient_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_concerns_therapist   ON enrollment_concerns (therapist_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_concerns_status      ON enrollment_concerns (status);

--rollback DROP TABLE enrollment_concerns;
