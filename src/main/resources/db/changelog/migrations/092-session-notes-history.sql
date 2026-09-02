--liquibase formatted sql

--changeset simplehearing:092-session-notes-history
-- One row per edit of a session's feedback/progress report/notes/performance score — captures
-- what those fields held right before the edit, so re-editing an already-saved session's notes
-- (e.g. on a later date) leaves an audit trail of who changed what and when.
CREATE TABLE IF NOT EXISTS session_notes_history (
    id                          UUID          NOT NULL PRIMARY KEY,
    org_id                      UUID          NOT NULL,
    session_id                  UUID          NOT NULL REFERENCES therapy_sessions (id) ON DELETE CASCADE,
    changed_by                  UUID          NOT NULL REFERENCES users (id),
    changed_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    previous_feedback           TEXT,
    previous_progress_report    TEXT,
    previous_notes              TEXT,
    previous_performance_score  INT
);
CREATE INDEX IF NOT EXISTS idx_session_notes_history_session ON session_notes_history (session_id, changed_at DESC);

--rollback DROP TABLE session_notes_history;
