--liquibase formatted sql

--changeset simplehearing:068-enrollments-discharge-link
-- The entire "episode of care" boundary: every enrollment gets stamped with the discharge
-- record that closed it (including CANCELLED ones, so leftovers from an earlier episode never
-- leak into a later one's report). An enrollment with a NULL value here belongs to the
-- patient's current, still-open episode.
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS discharged_in_record_id UUID;
CREATE INDEX IF NOT EXISTS idx_enrollments_discharged_in_record ON enrollments (discharged_in_record_id);

--rollback DROP INDEX IF EXISTS idx_enrollments_discharged_in_record; ALTER TABLE enrollments DROP COLUMN IF EXISTS discharged_in_record_id;
