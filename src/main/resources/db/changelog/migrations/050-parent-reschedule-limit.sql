--liquibase formatted sql

--changeset simplehearing:050-parent-reschedule-limit
-- A parent may move only a few sessions per therapy plan. The existing evidence of a
-- parent request (reschedule_reason / reschedule_requested_by) is cleared the moment an
-- admin actions it, so counting live state would let the allowance be spent endlessly.
-- This flag is set once and never cleared, so the count survives the reschedule.
ALTER TABLE therapy_sessions
    ADD COLUMN IF NOT EXISTS parent_reschedule_requested BOOLEAN NOT NULL DEFAULT false;

-- Requests still awaiting action are the only history that can be recovered.
UPDATE therapy_sessions
   SET parent_reschedule_requested = true
 WHERE reschedule_reason = 'PARENT_REQUEST';

CREATE INDEX IF NOT EXISTS idx_sessions_enrollment_parent_resched
    ON therapy_sessions (enrollment_id)
 WHERE parent_reschedule_requested;

--rollback DROP INDEX IF EXISTS idx_sessions_enrollment_parent_resched;
--rollback ALTER TABLE therapy_sessions DROP COLUMN parent_reschedule_requested;
