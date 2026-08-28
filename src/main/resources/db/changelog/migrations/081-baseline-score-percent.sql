--liquibase formatted sql

--changeset simplehearing:081-baseline-score-percent
ALTER TABLE baseline_domain_values ADD COLUMN IF NOT EXISTS score_percent INTEGER;
ALTER TABLE baseline_domain_values
    ADD CONSTRAINT chk_baseline_domain_value_score CHECK (score_percent IS NULL OR score_percent BETWEEN 0 AND 100);

ALTER TABLE baseline_progress_entries ADD COLUMN IF NOT EXISTS score_percent INTEGER;
ALTER TABLE baseline_progress_entries
    ADD CONSTRAINT chk_baseline_progress_score CHECK (score_percent IS NULL OR score_percent BETWEEN 0 AND 100);

--rollback ALTER TABLE baseline_domain_values DROP CONSTRAINT chk_baseline_domain_value_score; ALTER TABLE baseline_domain_values DROP COLUMN score_percent;
--rollback ALTER TABLE baseline_progress_entries DROP CONSTRAINT chk_baseline_progress_score; ALTER TABLE baseline_progress_entries DROP COLUMN score_percent;
