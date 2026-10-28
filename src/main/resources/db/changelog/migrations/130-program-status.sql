--liquibase formatted sql

--changeset simplehearing:130-program-status
-- The admin-set lifecycle label of a program (separate from the enrollment's own status and its care status):
-- ACTIVE, RENEWAL_DUE (set automatically once renewal reminders start) or COMPLETED_DISCONTINUED (stops them).
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS program_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
-- Which morning/evening reminder was sent last ("2026-10-28-AM"), so a restart or double run never repeats a slot.
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS renewal_reminder_last_slot VARCHAR(20);

-- Programs that are already over must not start sending renewal reminders the day this ships.
UPDATE enrollments SET program_status = 'COMPLETED_DISCONTINUED'
WHERE status IN ('COMPLETED', 'CANCELLED') OR discharged_in_record_id IS NOT NULL;

--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS renewal_reminder_last_slot;
--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS program_status;
