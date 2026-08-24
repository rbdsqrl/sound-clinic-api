--liquibase formatted sql

--changeset simplehearing:061-enrollment-care-status
-- Care status is a therapist/admin-set clinical-health signal on an ACTIVE enrollment,
-- separate from Enrollment.status (ACTIVE/COMPLETED/CANCELLED) and separate from the
-- patient's own stage (which carries DISCHARGED — a program is "completed", a patient is "discharged").
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS care_status VARCHAR NOT NULL DEFAULT 'ON_TRACK';
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS care_status_note TEXT;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS care_status_updated_by UUID;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS care_status_updated_at TIMESTAMP;

--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS care_status; ALTER TABLE enrollments DROP COLUMN IF EXISTS care_status_note; ALTER TABLE enrollments DROP COLUMN IF EXISTS care_status_updated_by; ALTER TABLE enrollments DROP COLUMN IF EXISTS care_status_updated_at;
