--liquibase formatted sql

--changeset simplehearing:095-therapist-reassignments
-- One bulk hand-off of a therapist's cases to another therapist — permanent, or bounded to a
-- start/end window that auto-reverts. Admin Roles (CLINIC_HEAD, OFFICE_ADMIN, BUSINESS_OWNER)
-- trigger these from the Members page.
CREATE TABLE IF NOT EXISTS therapist_reassignments (
    id                 UUID PRIMARY KEY,
    org_id             UUID NOT NULL,
    from_therapist_id  UUID NOT NULL,
    to_therapist_id    UUID NOT NULL,
    type               VARCHAR(20) NOT NULL,
    start_date         DATE NOT NULL,
    end_date           DATE,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reason             TEXT,
    created_by         UUID NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    reverted_at        TIMESTAMP WITH TIME ZONE,
    reverted_by        UUID,

    CONSTRAINT fk_reassignment_org  FOREIGN KEY (org_id) REFERENCES organisations (id) ON DELETE CASCADE,
    CONSTRAINT fk_reassignment_from FOREIGN KEY (from_therapist_id) REFERENCES users (id),
    CONSTRAINT fk_reassignment_to   FOREIGN KEY (to_therapist_id)   REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_reassignment_org_status ON therapist_reassignments (org_id, status);
CREATE INDEX IF NOT EXISTS idx_reassignment_endcheck   ON therapist_reassignments (status, type, end_date);
CREATE INDEX IF NOT EXISTS idx_reassignment_from       ON therapist_reassignments (from_therapist_id);
CREATE INDEX IF NOT EXISTS idx_reassignment_to         ON therapist_reassignments (to_therapist_id);

--rollback DROP TABLE therapist_reassignments;

--changeset simplehearing:095-therapist-reassignment-cases
-- One row per patient touched by a reassignment batch — drives the precise, per-case revert.
CREATE TABLE IF NOT EXISTS therapist_reassignment_cases (
    id               UUID PRIMARY KEY,
    reassignment_id  UUID NOT NULL,
    patient_id       UUID NOT NULL,
    enrollment_id    UUID,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_trc_reassignment FOREIGN KEY (reassignment_id) REFERENCES therapist_reassignments (id) ON DELETE CASCADE,
    CONSTRAINT fk_trc_patient      FOREIGN KEY (patient_id)      REFERENCES patients (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_trc_reassignment ON therapist_reassignment_cases (reassignment_id);
CREATE INDEX IF NOT EXISTS idx_trc_patient      ON therapist_reassignment_cases (patient_id);

--rollback DROP TABLE therapist_reassignment_cases;
