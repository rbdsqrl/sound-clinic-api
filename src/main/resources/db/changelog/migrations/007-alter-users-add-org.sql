--liquibase formatted sql

--changeset simplehearing:007-alter-users-add-org
ALTER TABLE users ADD COLUMN org_id UUID;
ALTER TABLE users ADD CONSTRAINT fk_users_org_id FOREIGN KEY (org_id) REFERENCES organisations (id) ON DELETE CASCADE;
CREATE INDEX idx_users_org_id ON users (org_id);

-- BUSINESS_OWNER belongs to an org, not a specific clinic — make clinic_id optional
ALTER TABLE users ALTER COLUMN clinic_id DROP NOT NULL;

--rollback ALTER TABLE users ALTER COLUMN clinic_id SET NOT NULL; ALTER TABLE users DROP CONSTRAINT fk_users_org_id; ALTER TABLE users DROP COLUMN org_id;
