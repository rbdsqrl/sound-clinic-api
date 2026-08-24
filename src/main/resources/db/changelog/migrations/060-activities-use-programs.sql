--liquibase formatted sql

--changeset simplehearing:060-activities-use-programs
-- An activity's "Therapy" field should draw from the same Programs a clinic already runs
-- (Manage → Programs) rather than a separate, unused Therapies lookup.
ALTER TABLE activities DROP CONSTRAINT IF EXISTS fk_activities_therapy;
ALTER TABLE activities RENAME COLUMN therapy_id TO program_id;
UPDATE activities SET program_id = NULL;
ALTER TABLE activities ADD CONSTRAINT fk_activities_program FOREIGN KEY (program_id) REFERENCES programs (id);

DROP INDEX IF EXISTS idx_activities_therapy;
CREATE INDEX IF NOT EXISTS idx_activities_program ON activities (program_id);

--rollback ALTER TABLE activities DROP CONSTRAINT IF EXISTS fk_activities_program; ALTER TABLE activities RENAME COLUMN program_id TO therapy_id;
