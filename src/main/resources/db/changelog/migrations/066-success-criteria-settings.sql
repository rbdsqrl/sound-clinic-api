--liquibase formatted sql

--changeset simplehearing:066-success-criteria-settings
-- Discharge success-criteria thresholds, editable per org later via an org-settings screen —
-- same shape as ai_provider/ai_api_key in 059. Defaults: 90% goal mastery, 70% parent
-- satisfaction, and every enrollment in the discharge episode must meet criteria (AND, not OR).
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS goal_mastery_threshold_pct INTEGER NOT NULL DEFAULT 90;
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS parent_satisfaction_threshold_pct INTEGER NOT NULL DEFAULT 70;
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS require_all_enrollments_for_discharge BOOLEAN NOT NULL DEFAULT TRUE;

--rollback ALTER TABLE organisations DROP COLUMN IF EXISTS goal_mastery_threshold_pct; ALTER TABLE organisations DROP COLUMN IF EXISTS parent_satisfaction_threshold_pct; ALTER TABLE organisations DROP COLUMN IF EXISTS require_all_enrollments_for_discharge;
