--liquibase formatted sql

--changeset simplehearing:032-task-multi-assignee
CREATE TABLE task_assignees (
    task_id  UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, user_id)
);

CREATE INDEX idx_task_assignees_task_id ON task_assignees(task_id);
CREATE INDEX idx_task_assignees_user_id ON task_assignees(user_id);

INSERT INTO task_assignees (task_id, user_id)
SELECT id, assigned_to FROM tasks WHERE assigned_to IS NOT NULL;

ALTER TABLE tasks DROP COLUMN assigned_to;

--rollback DROP TABLE task_assignees;
