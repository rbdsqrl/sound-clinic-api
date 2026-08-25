--liquibase formatted sql

--changeset simplehearing:072-organisation-weekly-off-days
-- Weekly recurring off days for an org (e.g. every Sunday) — skipped by session/review-meeting
-- autoscheduling the same way public holidays already are. Ad-hoc sessions are unaffected since
-- they're booked through a separate, non-generated flow.
CREATE TABLE IF NOT EXISTS organisation_weekly_off_days (
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    PRIMARY KEY (organisation_id, day_of_week)
);

--rollback DROP TABLE IF EXISTS organisation_weekly_off_days;
