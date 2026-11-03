--liquibase formatted sql

--changeset simplehearing:134-session-cancelled-with-program
-- Marks sessions cancelled because their whole program was cancelled or closed by a discharge, so the calendar can
-- hide them instead of showing a cancelled chip beside the sessions that really run. Backfills the upcoming ones
-- already cancelled that way (migration 133 and the cancel/discharge fix); older cancellations stay visible history.
ALTER TABLE therapy_sessions ADD COLUMN IF NOT EXISTS cancelled_with_program BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE therapy_sessions
SET cancelled_with_program = TRUE
WHERE status = 'CANCELLED'
  AND session_date >= CURRENT_DATE - 1
  AND enrollment_id IN (SELECT id FROM enrollments WHERE (status = 'CANCELLED' AND cancelled_by_case_inactive = FALSE)
                         OR (status = 'COMPLETED' AND discharged_in_record_id IS NOT NULL));

--rollback ALTER TABLE therapy_sessions DROP COLUMN IF EXISTS cancelled_with_program;
