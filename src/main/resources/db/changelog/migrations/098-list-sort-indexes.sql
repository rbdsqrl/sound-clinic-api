--liquibase formatted sql

--changeset simplehearing:098-list-sort-indexes
-- Paginated Cases/Members lists sort by created_at desc within an org. Without this,
-- that ORDER BY has no supporting index and Postgres sorts the whole matching set in
-- memory on every page load.
CREATE INDEX IF NOT EXISTS idx_patients_org_created ON patients (org_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_org_created    ON users (org_id, created_at DESC);

--rollback DROP INDEX idx_patients_org_created; DROP INDEX idx_users_org_created;
