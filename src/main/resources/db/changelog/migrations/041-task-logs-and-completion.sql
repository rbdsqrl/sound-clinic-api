--liquibase formatted sql

--changeset simplehearing:041-task-logs-and-completion
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

CREATE TABLE task_logs (
    id          UUID         PRIMARY KEY,
    org_id      UUID         NOT NULL,
    task_id     UUID         NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    log_type    VARCHAR(50)  NOT NULL,
    actor_id    UUID         NOT NULL,
    actor_name  VARCHAR(255) NOT NULL,
    details     TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX ON task_logs(task_id);

--rollback DROP TABLE IF EXISTS task_logs; ALTER TABLE tasks DROP COLUMN IF EXISTS completed_at;
