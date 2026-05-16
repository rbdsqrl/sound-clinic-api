--liquibase formatted sql

--changeset simplehearing:030-create-task-comments
CREATE TABLE task_comments (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      UUID      NOT NULL,
    task_id     UUID      NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_id   UUID      NOT NULL REFERENCES users(id),
    body        TEXT      NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);

--rollback DROP TABLE task_comments;
