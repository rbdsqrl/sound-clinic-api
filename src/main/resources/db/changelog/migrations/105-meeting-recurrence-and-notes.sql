--liquibase formatted sql

--changeset simplehearing:105-meeting-recurrence-and-notes
-- Per-occurrence notes (post-meeting write-up), separate from `description` which is the
-- shared pre-meeting agenda set once at creation time.
ALTER TABLE meetings ADD COLUMN IF NOT EXISTS notes TEXT;

-- A recurring meeting is generated as one row per occurrence, all sharing series_id. NULL on
-- every column below means a plain one-off meeting (the existing behaviour, unchanged).
ALTER TABLE meetings ADD COLUMN IF NOT EXISTS series_id UUID;
ALTER TABLE meetings ADD COLUMN IF NOT EXISTS occurrence_number INTEGER;
ALTER TABLE meetings ADD COLUMN IF NOT EXISTS total_occurrences INTEGER;

CREATE INDEX IF NOT EXISTS idx_meetings_series_id ON meetings (series_id);

--rollback ALTER TABLE meetings DROP COLUMN IF EXISTS notes;
--rollback ALTER TABLE meetings DROP COLUMN IF EXISTS series_id;
--rollback ALTER TABLE meetings DROP COLUMN IF EXISTS occurrence_number;
--rollback ALTER TABLE meetings DROP COLUMN IF EXISTS total_occurrences;
