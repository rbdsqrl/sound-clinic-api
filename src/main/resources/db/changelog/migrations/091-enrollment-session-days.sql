--liquibase formatted sql

--changeset simplehearing:091-enrollment-session-days
-- Which days of the week this plan's sessions land on — chosen when the schedule is set up.
-- Empty/no rows = no restriction (every day is a candidate, same as current behaviour), so
-- existing enrollments keep working unchanged.
CREATE TABLE IF NOT EXISTS enrollment_session_days (
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    PRIMARY KEY (enrollment_id, day_of_week)
);

--rollback DROP TABLE IF EXISTS enrollment_session_days;
