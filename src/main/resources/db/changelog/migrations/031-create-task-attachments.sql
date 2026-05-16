--liquibase formatted sql

--changeset simplehearing:031-create-task-attachments
CREATE TABLE task_attachments (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id           UUID          NOT NULL,
    task_id          UUID          NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    uploaded_by      UUID          NOT NULL REFERENCES users(id),
    file_name        VARCHAR(255)  NOT NULL,
    file_url         VARCHAR(1000) NOT NULL,
    content_type     VARCHAR(100),
    file_size_bytes  BIGINT,
    created_at       TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_attachments_task_id ON task_attachments(task_id);

--rollback DROP TABLE task_attachments;
