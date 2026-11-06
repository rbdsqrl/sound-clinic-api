--liquibase formatted sql

--changeset simplehearing:140-attendance-edit-history
-- The Business Owner can correct an employee's login / logout time by hand. Each correction is kept as a row here —
-- who, when, and what changed (JSON list of field / from / to) — and the attendance record is flagged as edited.
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS manually_edited BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS attendance_edit_history (
    id            UUID          NOT NULL PRIMARY KEY,
    org_id        UUID          NOT NULL,
    attendance_id UUID          NOT NULL REFERENCES attendance (id) ON DELETE CASCADE,
    edited_by     UUID          REFERENCES users (id) ON DELETE SET NULL,
    edited_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    note          VARCHAR(500),
    changes       TEXT          NOT NULL    -- JSON: [{"field":"Login time","from":"09:20","to":"09:00"}]
);
CREATE INDEX IF NOT EXISTS idx_attendance_edit_history_record ON attendance_edit_history (attendance_id, edited_at DESC);

--rollback DROP TABLE attendance_edit_history;
--rollback ALTER TABLE attendance DROP COLUMN IF EXISTS manually_edited;
