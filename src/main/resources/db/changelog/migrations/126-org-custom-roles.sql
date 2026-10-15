--liquibase formatted sql

--changeset simplehearing:126-org-custom-roles
-- Organisation-defined "Custom Roles" (e.g. Speech Language Pathologist) that an admin can pick for a member.
-- A member's chosen name is stored on users.specialization and shown in place of their role name; this table
-- is only the picker list, so tidying it never changes what members already have.
CREATE TABLE IF NOT EXISTS org_custom_roles (
    id          UUID          NOT NULL PRIMARY KEY,
    org_id      UUID          NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    created_by  UUID,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_org_custom_roles_org_name ON org_custom_roles (org_id, lower(name));

--rollback DROP TABLE IF EXISTS org_custom_roles;
