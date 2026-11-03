--liquibase formatted sql

--changeset simplehearing:133-cancel-orphaned-enrollment-sessions
-- Cancelling a single enrollment, discharging a patient, or force-completing a program used to leave its upcoming
-- sessions and review meetings live, so they kept showing on calendars and in reports. Cancel the ones still ahead on
-- enrollments that are already closed that way. Enrollments cancelled because the case went inactive are left alone:
-- their sessions are already cancelled and restorable.
UPDATE therapy_sessions
SET status = 'CANCELLED'
WHERE session_date >= CURRENT_DATE
  AND status IN ('SCHEDULED', 'PENDING_RESCHEDULE', 'CANCELLATION_REQUESTED')
  AND enrollment_id IN (SELECT id FROM enrollments WHERE (status = 'CANCELLED' AND cancelled_by_case_inactive = FALSE)
                         OR (status = 'COMPLETED' AND discharged_in_record_id IS NOT NULL));

UPDATE review_meetings
SET status = 'CANCELLED', cancelled_reason = 'Program cancelled', ics_sequence = ics_sequence + 1
WHERE meeting_date >= CURRENT_DATE
  AND status = 'SCHEDULED'
  AND enrollment_id IN (SELECT id FROM enrollments WHERE (status = 'CANCELLED' AND cancelled_by_case_inactive = FALSE)
                         OR (status = 'COMPLETED' AND (discharged_in_record_id IS NOT NULL OR care_status = 'PROGRAM_COMPLETED')));

--rollback SELECT 1;
