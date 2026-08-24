--liquibase formatted sql

--changeset simplehearing:065-enrollment-therapist-signoff
-- One of three success criteria a discharge is measured against — the assigned therapist
-- explicitly confirming the program's goals were met, alongside goal-mastery and parent-
-- satisfaction thresholds.
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS therapist_signed_off BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS therapist_signoff_by UUID;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS therapist_signoff_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS therapist_signoff_notes TEXT;

--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS therapist_signed_off; ALTER TABLE enrollments DROP COLUMN IF EXISTS therapist_signoff_by; ALTER TABLE enrollments DROP COLUMN IF EXISTS therapist_signoff_at; ALTER TABLE enrollments DROP COLUMN IF EXISTS therapist_signoff_notes;
