--liquibase formatted sql

--changeset simplehearing:132-parent-session-feedback
-- A parent's written feedback on one completed session. Admin roles see it first (admin_reviewed_at is null until one
-- reads it); an admin can share it with the session's therapist (shared_with_therapist), who then sees it until they
-- mark it seen. One feedback per parent per session — a parent edits their own.
CREATE TABLE IF NOT EXISTS parent_session_feedback (
    id                     UUID PRIMARY KEY,
    org_id                 UUID NOT NULL,
    session_id             UUID NOT NULL REFERENCES therapy_sessions(id) ON DELETE CASCADE,
    patient_id             UUID NOT NULL,
    parent_id              UUID NOT NULL,
    message                TEXT NOT NULL,
    created_at             TIMESTAMP NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP NOT NULL DEFAULT now(),
    admin_reviewed_at      TIMESTAMP,
    admin_reviewed_by      UUID,
    shared_with_therapist  BOOLEAN NOT NULL DEFAULT FALSE,
    shared_at              TIMESTAMP,
    shared_by              UUID,
    therapist_seen_at      TIMESTAMP,
    CONSTRAINT uq_parent_feedback_session_parent UNIQUE (session_id, parent_id)
);
CREATE INDEX IF NOT EXISTS idx_parent_feedback_org_review ON parent_session_feedback (org_id, admin_reviewed_at);
CREATE INDEX IF NOT EXISTS idx_parent_feedback_patient ON parent_session_feedback (patient_id);

--rollback DROP TABLE IF EXISTS parent_session_feedback;
