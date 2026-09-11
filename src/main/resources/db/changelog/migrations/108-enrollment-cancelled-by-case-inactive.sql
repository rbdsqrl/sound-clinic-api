--liquibase formatted sql

--changeset simplehearing:108-enrollment-cancelled-by-case-inactive
-- Marks an enrollment (program) that was auto-cancelled because an Admin Role marked its
-- patient's case inactive, as opposed to a manual cancel via PATCH /enrollments/{id}/cancel.
-- Lets marking the case active again restore precisely these enrollments to ACTIVE, and no
-- others — mirrors cancelled_by_case_inactive on therapy_sessions (106) and review_meetings (107).
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS cancelled_by_case_inactive BOOLEAN NOT NULL DEFAULT FALSE;

--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS cancelled_by_case_inactive;
