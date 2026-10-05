--liquibase formatted sql

--changeset simplehearing:117-session-activity-events
-- One row per thing that happened to a session — marked completed, cancelled, rescheduled, a score
-- given, notes saved or edited — with the before/after values as a JSON list. Complements
-- session_notes_history (092), which only keeps the prior version of notes at each edit.
CREATE TABLE IF NOT EXISTS session_activity_events (
    id          UUID          NOT NULL PRIMARY KEY,
    org_id      UUID          NOT NULL,
    session_id  UUID          NOT NULL REFERENCES therapy_sessions (id) ON DELETE CASCADE,
    actor_id    UUID          REFERENCES users (id), -- NULL = the system (e.g. an automatic cancellation)
    event_type  VARCHAR(40)   NOT NULL,
    summary     VARCHAR(255)  NOT NULL,
    changes     TEXT,                                -- JSON: [{"field":"Status","from":"Scheduled","to":"Completed"}]
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_session_activity_session ON session_activity_events (session_id, created_at DESC);

--rollback DROP TABLE session_activity_events;
