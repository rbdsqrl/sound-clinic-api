--liquibase formatted sql

--changeset simplehearing:057-create-activity-resources
CREATE TABLE IF NOT EXISTS activity_resources (
    id               UUID          NOT NULL PRIMARY KEY,
    org_id           UUID          NOT NULL,
    activity_id      UUID          NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    uploaded_by      UUID          NOT NULL REFERENCES users (id),
    file_name        VARCHAR(255)  NOT NULL,
    file_url         VARCHAR(1000) NOT NULL,
    content_type     VARCHAR(100),
    file_size_bytes  BIGINT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_activity_resources_activity ON activity_resources (activity_id);

CREATE TABLE IF NOT EXISTS activity_links (
    id          UUID NOT NULL PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL DEFAULT 0,
    url         VARCHAR(1000) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_activity_links_activity ON activity_links (activity_id);

--rollback DROP TABLE activity_links; DROP TABLE activity_resources;
