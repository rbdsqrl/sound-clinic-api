--liquibase formatted sql

--changeset simplehearing:029-create-tasks
CREATE TABLE tasks (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID         NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    assigned_to     UUID         NOT NULL REFERENCES users(id),
    assigned_by     UUID         NOT NULL REFERENCES users(id),
    due_date        DATE,
    priority        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_org_id          ON tasks(org_id);
CREATE INDEX idx_tasks_assigned_to     ON tasks(assigned_to);
CREATE INDEX idx_tasks_assigned_by     ON tasks(assigned_by);
CREATE INDEX idx_tasks_status          ON tasks(org_id, status);

--rollback DROP TABLE tasks;
