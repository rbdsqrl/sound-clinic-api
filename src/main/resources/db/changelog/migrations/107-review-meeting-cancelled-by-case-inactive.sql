--liquibase formatted sql

--changeset simplehearing:107-review-meeting-cancelled-by-case-inactive
-- Marks a review meeting that was auto-cancelled because an Admin Role marked the case
-- inactive (patients.is_active = false), as opposed to being cancelled for any other reason.
-- Lets marking the case active again restore precisely these review meetings to SCHEDULED,
-- and no others — mirrors cancelled_by_case_inactive (106) on therapy_sessions, which the
-- same case-inactive toggle already applied to sessions but not to review meetings.
ALTER TABLE review_meetings ADD COLUMN IF NOT EXISTS cancelled_by_case_inactive BOOLEAN NOT NULL DEFAULT FALSE;

--rollback ALTER TABLE review_meetings DROP COLUMN IF EXISTS cancelled_by_case_inactive;
