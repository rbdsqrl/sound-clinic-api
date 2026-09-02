--liquibase formatted sql

--changeset simplehearing:093-enrollment-manual-success-criteria
-- Goal mastery and parent satisfaction are normally computed from IEP trial logs and review
-- meeting ratings — both go blank forever on a program that was force-completed by an admin
-- override rather than finishing the normal way. These let that override carry a manually
-- entered value instead, which the success-criteria computation prefers over the derived one
-- whenever it's set.
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS manual_goal_mastery_pct DOUBLE PRECISION;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS manual_parent_satisfaction_pct DOUBLE PRECISION;

--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS manual_goal_mastery_pct; ALTER TABLE enrollments DROP COLUMN IF EXISTS manual_parent_satisfaction_pct;
