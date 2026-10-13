--liquibase formatted sql

--changeset simplehearing:125-audit-log
-- One row per change made to a member, parent or case profile — who did what, with before/after
-- values as a JSON list. subject_id points at users (members and parents) or patients (cases)
-- depending on subject_type, so it carries no foreign key; entries stay after the subject is removed.
CREATE TABLE IF NOT EXISTS audit_log_entries (
    id            UUID          NOT NULL PRIMARY KEY,
    org_id        UUID          NOT NULL,
    subject_type  VARCHAR(20)   NOT NULL,            -- USER | CASE
    subject_id    UUID          NOT NULL,
    actor_id      UUID          REFERENCES users (id) ON DELETE SET NULL, -- NULL = the system or the person themselves before an account existed
    event_type    VARCHAR(40)   NOT NULL,
    summary       VARCHAR(255)  NOT NULL,
    changes       TEXT,                              -- JSON: [{"field":"Phone","from":"123","to":"456"}]
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_log_subject ON audit_log_entries (org_id, subject_type, subject_id, created_at DESC);

--rollback DROP TABLE audit_log_entries;
