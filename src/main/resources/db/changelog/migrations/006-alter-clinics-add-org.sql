--liquibase formatted sql

--changeset simplehearing:006-alter-clinics-add-org
ALTER TABLE clinics ADD COLUMN org_id UUID;
ALTER TABLE clinics ADD CONSTRAINT fk_clinics_org_id FOREIGN KEY (org_id) REFERENCES organisations (id) ON DELETE CASCADE;
CREATE INDEX idx_clinics_org_id ON clinics (org_id);

-- Subdomain is now an org-level concept; remove it from clinics
ALTER TABLE clinics DROP CONSTRAINT uq_clinics_subdomain;
ALTER TABLE clinics DROP COLUMN subdomain;

--rollback ALTER TABLE clinics ADD COLUMN subdomain VARCHAR(100); ALTER TABLE clinics DROP CONSTRAINT fk_clinics_org_id; ALTER TABLE clinics DROP COLUMN org_id;
