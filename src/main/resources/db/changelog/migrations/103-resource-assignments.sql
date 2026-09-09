--liquibase formatted sql

--changeset simplehearing:103-resource-assignments
CREATE TABLE IF NOT EXISTS resource_assignments (
    id            UUID          NOT NULL PRIMARY KEY,
    org_id        UUID          NOT NULL,
    resource_id   UUID          NOT NULL REFERENCES resources (id) ON DELETE CASCADE,
    patient_id    UUID          NOT NULL REFERENCES patients (id) ON DELETE CASCADE,
    assigned_by   UUID          NOT NULL REFERENCES users (id),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (resource_id, patient_id)
);
CREATE INDEX IF NOT EXISTS idx_resource_assignments_org ON resource_assignments (org_id);
CREATE INDEX IF NOT EXISTS idx_resource_assignments_patient ON resource_assignments (patient_id);
CREATE INDEX IF NOT EXISTS idx_resource_assignments_resource ON resource_assignments (resource_id);

--rollback DROP TABLE resource_assignments;
