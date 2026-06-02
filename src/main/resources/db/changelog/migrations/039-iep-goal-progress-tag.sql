--liquibase formatted sql

--changeset simplehearing:039-iep-goal-progress-tag
ALTER TABLE iep_goals ADD COLUMN IF NOT EXISTS progress_tag VARCHAR(2);

--rollback ALTER TABLE iep_goals DROP COLUMN IF EXISTS progress_tag;
