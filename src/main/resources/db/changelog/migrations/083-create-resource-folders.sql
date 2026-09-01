--liquibase formatted sql

--changeset simplehearing:083-create-resource-folders
CREATE TABLE IF NOT EXISTS resource_folders (
    id                UUID          NOT NULL PRIMARY KEY,
    org_id            UUID          NOT NULL,
    parent_folder_id  UUID          REFERENCES resource_folders (id) ON DELETE CASCADE,
    name              VARCHAR(255)  NOT NULL,
    created_by        UUID          NOT NULL REFERENCES users (id),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_resource_folders_org ON resource_folders (org_id);
CREATE INDEX IF NOT EXISTS idx_resource_folders_parent ON resource_folders (parent_folder_id);

--rollback DROP TABLE resource_folders;
