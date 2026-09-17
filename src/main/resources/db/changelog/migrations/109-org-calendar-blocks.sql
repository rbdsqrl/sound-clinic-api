--liquibase formatted sql

--changeset simplehearing:109-org-calendar-blocks
-- Org-wide recurring calendar blocks (e.g. "Lunch Break") — a rule, not materialized rows, the
-- same approach as organisation_weekly_off_days: the calendar expands it on the fly for whatever
-- date range is being viewed. Shown on every authenticated user's calendar automatically, with no
-- participant list to maintain (unlike Meetings) and, unlike Public Holidays, it does not block
-- session/review-meeting autoscheduling — it's a shared visibility marker, not a day off.
CREATE TABLE IF NOT EXISTS org_calendar_blocks (
    id         UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id     UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL,
    start_date DATE NOT NULL,
    end_date   DATE,                 -- NULL = ongoing indefinitely
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_org_calendar_blocks_time CHECK (end_time > start_time),
    CONSTRAINT chk_org_calendar_blocks_dates CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX IF NOT EXISTS idx_org_calendar_blocks_org ON org_calendar_blocks (org_id);

CREATE TABLE IF NOT EXISTS org_calendar_block_days (
    org_calendar_block_id UUID NOT NULL REFERENCES org_calendar_blocks(id) ON DELETE CASCADE,
    day_of_week            VARCHAR(10) NOT NULL,
    PRIMARY KEY (org_calendar_block_id, day_of_week)
);

--rollback DROP TABLE IF EXISTS org_calendar_block_days; DROP TABLE IF EXISTS org_calendar_blocks;
