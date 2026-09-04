--liquibase formatted sql

--changeset simplehearing:100-activity-linked-resources
CREATE TABLE IF NOT EXISTS activity_linked_resources (
    id            UUID NOT NULL PRIMARY KEY,
    activity_id   UUID NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    resource_id   UUID NOT NULL REFERENCES resources (id) ON DELETE CASCADE,
    order_index   INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (activity_id, resource_id)
);
CREATE INDEX IF NOT EXISTS idx_activity_linked_resources_activity ON activity_linked_resources (activity_id);
CREATE INDEX IF NOT EXISTS idx_activity_linked_resources_resource ON activity_linked_resources (resource_id);

--rollback DROP TABLE activity_linked_resources;
