--liquibase formatted sql

--changeset simplehearing:084-create-resources
CREATE TABLE IF NOT EXISTS resources (
    id           UUID          NOT NULL PRIMARY KEY,
    org_id       UUID          NOT NULL,
    folder_id    UUID          REFERENCES resource_folders (id) ON DELETE CASCADE,
    name         VARCHAR(255)  NOT NULL,
    type         VARCHAR(20)   NOT NULL, -- LINK | VIDEO | IMAGE
    url          VARCHAR(2000) NOT NULL,
    created_by   UUID          NOT NULL REFERENCES users (id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_resources_org ON resources (org_id);
CREATE INDEX IF NOT EXISTS idx_resources_folder ON resources (folder_id);

--rollback DROP TABLE resources;
