--liquibase formatted sql

--changeset simplehearing:106-session-cancelled-by-case-inactive
-- Marks a session that was auto-cancelled because an Admin Role marked the case inactive
-- (patients.is_active = false), as opposed to being cancelled for any other reason. Lets
-- marking the case active again restore precisely these sessions to SCHEDULED, and no others —
-- mirrors cancelled_by_program_completion (094) at the patient level instead of the enrollment
-- level.
ALTER TABLE therapy_sessions ADD COLUMN IF NOT EXISTS cancelled_by_case_inactive BOOLEAN NOT NULL DEFAULT FALSE;

--rollback ALTER TABLE therapy_sessions DROP COLUMN IF EXISTS cancelled_by_case_inactive;
