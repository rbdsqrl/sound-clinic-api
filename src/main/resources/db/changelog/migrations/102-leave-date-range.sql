--liquibase formatted sql

--changeset simplehearing:102-leave-date-range
-- Half-day leave is being dropped — collapse any existing HALF_DAY rows to FULL_DAY before the
-- Java enum constant that names them goes away (Hibernate would otherwise fail to deserialize
-- an old row's stored "HALF_DAY" string once LeaveType no longer has that constant).
UPDATE leaves SET leave_type = 'FULL_DAY' WHERE leave_type = 'HALF_DAY';

-- A leave request now spans a range (leave_date = start) rather than a single day. Backfill
-- end_date = leave_date for existing rows so every leave has a concrete (possibly one-day) range.
ALTER TABLE leaves ADD COLUMN IF NOT EXISTS end_date DATE;
UPDATE leaves SET end_date = leave_date WHERE end_date IS NULL;
ALTER TABLE leaves ALTER COLUMN end_date SET NOT NULL;

-- Traceability for the "Needs Rescheduling" dashboard card — when a session's reschedule was
-- caused by an approved leave, record that leave's range alongside the existing reschedule_reason
-- so the UI can show e.g. "Therapist leave · Sep 5 - Sep 12" instead of just "Therapist leave".
ALTER TABLE therapy_sessions ADD COLUMN IF NOT EXISTS reschedule_leave_start_date DATE;
ALTER TABLE therapy_sessions ADD COLUMN IF NOT EXISTS reschedule_leave_end_date DATE;

--rollback ALTER TABLE therapy_sessions DROP COLUMN IF EXISTS reschedule_leave_end_date;
--rollback ALTER TABLE therapy_sessions DROP COLUMN IF EXISTS reschedule_leave_start_date;
--rollback ALTER TABLE leaves ALTER COLUMN end_date DROP NOT NULL;
--rollback ALTER TABLE leaves DROP COLUMN IF EXISTS end_date;
