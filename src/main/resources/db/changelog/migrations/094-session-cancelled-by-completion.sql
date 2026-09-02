--liquibase formatted sql

--changeset simplehearing:094-session-cancelled-by-completion
-- Marks a session that was auto-cancelled because its enrollment was force-completed (the
-- Clinic Head/Office Admin/Business Owner "Mark as Completed" override), as opposed to being
-- cancelled for any other reason. Lets reactivating that enrollment restore precisely these
-- sessions to SCHEDULED, and no others.
ALTER TABLE therapy_sessions ADD COLUMN IF NOT EXISTS cancelled_by_program_completion BOOLEAN NOT NULL DEFAULT FALSE;

--rollback ALTER TABLE therapy_sessions DROP COLUMN IF EXISTS cancelled_by_program_completion;
