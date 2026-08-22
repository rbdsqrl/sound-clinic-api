--liquibase formatted sql

--changeset simplehearing:052-reschedule-history
-- Analytics counted "rescheduled" as sessions sitting in PENDING_RESCHEDULE, which is a
-- snapshot of what is awaiting action rather than a record of what actually moved: once
-- the clinic actions a request the session returns to SCHEDULED and the count drops to
-- zero. This counter is incremented on every completed move and never reset, so the
-- history survives.
ALTER TABLE therapy_sessions
    ADD COLUMN IF NOT EXISTS reschedule_count INTEGER NOT NULL DEFAULT 0;

--rollback ALTER TABLE therapy_sessions DROP COLUMN reschedule_count;
