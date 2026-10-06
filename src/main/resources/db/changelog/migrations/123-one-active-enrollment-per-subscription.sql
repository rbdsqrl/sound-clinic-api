--liquibase formatted sql

--changeset simplehearing:123-one-active-enrollment-per-subscription
-- Repeated "Confirm Enrollment" submits created several ACTIVE enrollments (each with its own full set of
-- sessions) on one subscription. Remove those duplicates with everything linked to them, then forbid it
-- happening again with a unique index.
--
-- A duplicate = any ACTIVE enrollment on a subscription other than the EARLIEST-created one (kept).
--   * Duplicates with no real activity (no COMPLETED / NO_SHOW session) are hard-deleted. Their
--     therapy_sessions (the calendar events) are deleted; review meetings, enrollment concerns, session-day
--     rows, session notes/attachments/activity events/feedback cascade; goal video evidence is nulled by its FK.
--   * Loosely linked rows with no FK are tidied: iep_plans / therapist_reassignment_cases are re-pointed to the
--     kept enrollment, evidence_uploads.session_id is cleared.
--   * Duplicates that DO have real session activity are never deleted — they are cancelled (with their open
--     sessions and review meetings) so history is preserved and the unique index can still be built.
CREATE TEMP TABLE dup_enrollments_123 ON COMMIT DROP AS
SELECT id, keeper_id FROM (
    SELECT id,
           FIRST_VALUE(id) OVER (PARTITION BY subscription_id ORDER BY created_at ASC, id ASC) AS keeper_id,
           ROW_NUMBER()    OVER (PARTITION BY subscription_id ORDER BY created_at ASC, id ASC) AS rn
    FROM enrollments WHERE status = 'ACTIVE'
) ranked WHERE rn > 1;

CREATE TEMP TABLE dup_with_activity_123 ON COMMIT DROP AS
SELECT DISTINCT d.id
FROM dup_enrollments_123 d
JOIN therapy_sessions ts ON ts.enrollment_id = d.id
WHERE ts.status IN ('COMPLETED', 'NO_SHOW');

CREATE TEMP TABLE dup_to_delete_123 ON COMMIT DROP AS
SELECT id, keeper_id FROM dup_enrollments_123 WHERE id NOT IN (SELECT id FROM dup_with_activity_123);

-- Re-point loosely linked rows at the kept enrollment; clear dangling session refs
UPDATE iep_plans ip SET enrollment_id = d.keeper_id
FROM dup_to_delete_123 d WHERE ip.enrollment_id = d.id;

UPDATE therapist_reassignment_cases c SET enrollment_id = d.keeper_id
FROM dup_to_delete_123 d WHERE c.enrollment_id = d.id;

UPDATE evidence_uploads SET session_id = NULL
WHERE session_id IN (SELECT ts.id FROM therapy_sessions ts WHERE ts.enrollment_id IN (SELECT id FROM dup_to_delete_123));

-- Delete (sessions first — the only FK without ON DELETE CASCADE)
DELETE FROM therapy_sessions WHERE enrollment_id IN (SELECT id FROM dup_to_delete_123);
DELETE FROM enrollments      WHERE id            IN (SELECT id FROM dup_to_delete_123);

-- Duplicates with real activity: cancel instead of delete
UPDATE therapy_sessions SET status = 'CANCELLED'
WHERE enrollment_id IN (SELECT id FROM dup_with_activity_123)
  AND status IN ('SCHEDULED', 'PENDING_RESCHEDULE', 'CANCELLATION_REQUESTED');

UPDATE review_meetings SET status = 'CANCELLED'
WHERE enrollment_id IN (SELECT id FROM dup_with_activity_123) AND status = 'SCHEDULED';

UPDATE enrollments SET status = 'CANCELLED' WHERE id IN (SELECT id FROM dup_with_activity_123);

CREATE UNIQUE INDEX IF NOT EXISTS uq_enrollments_one_active_per_subscription
    ON enrollments (subscription_id) WHERE status = 'ACTIVE';

--rollback DROP INDEX IF EXISTS uq_enrollments_one_active_per_subscription;
