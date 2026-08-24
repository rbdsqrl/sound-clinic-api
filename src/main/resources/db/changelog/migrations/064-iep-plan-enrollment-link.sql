--liquibase formatted sql

--changeset simplehearing:064-iep-plan-enrollment-link
-- Goal-mastery-per-program (needed for discharge success criteria, and for a patient with
-- concurrent programs) can't be computed precisely without knowing which enrollment an IEP
-- plan belongs to. Nullable — legacy plans have no reliable way to infer this retroactively
-- (no date-overlap heuristic is safe once concurrent enrollments exist); new plans created
-- from an enrollment context populate it going forward.
ALTER TABLE iep_plans ADD COLUMN IF NOT EXISTS enrollment_id UUID;
CREATE INDEX IF NOT EXISTS idx_iep_plans_enrollment ON iep_plans (enrollment_id);

--rollback DROP INDEX IF EXISTS idx_iep_plans_enrollment; ALTER TABLE iep_plans DROP COLUMN IF EXISTS enrollment_id;
