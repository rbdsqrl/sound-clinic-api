--liquibase formatted sql

--changeset simplehearing:058-create-activity-assignments
CREATE TABLE IF NOT EXISTS activity_assignments (
    id                     UUID        NOT NULL PRIMARY KEY,
    org_id                 UUID        NOT NULL,
    activity_id            UUID        NOT NULL REFERENCES activities (id),
    patient_id             UUID        NOT NULL REFERENCES patients (id) ON DELETE CASCADE,
    assigned_by            UUID        NOT NULL REFERENCES users (id),
    assigned_therapist_id  UUID        REFERENCES users (id),
    status                 VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    start_date             DATE,
    due_date               DATE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_activity_assignments_org      ON activity_assignments (org_id);
CREATE INDEX IF NOT EXISTS idx_activity_assignments_patient  ON activity_assignments (patient_id);
CREATE INDEX IF NOT EXISTS idx_activity_assignments_activity ON activity_assignments (activity_id);

CREATE TABLE IF NOT EXISTS activity_attempt_logs (
    id             UUID NOT NULL PRIMARY KEY,
    org_id         UUID NOT NULL,
    assignment_id  UUID NOT NULL REFERENCES activity_assignments (id) ON DELETE CASCADE,
    logged_by      UUID NOT NULL REFERENCES users (id),
    attempt_date   DATE NOT NULL,
    note           TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_activity_attempt_logs_assignment ON activity_attempt_logs (assignment_id);
CREATE INDEX IF NOT EXISTS idx_activity_attempt_logs_org_date   ON activity_attempt_logs (org_id, attempt_date);

CREATE TABLE IF NOT EXISTS activity_attempt_answers (
    id              UUID NOT NULL PRIMARY KEY,
    attempt_log_id  UUID NOT NULL REFERENCES activity_attempt_logs (id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES activity_checklist_questions (id),
    text_answer     TEXT
);
CREATE INDEX IF NOT EXISTS idx_activity_attempt_answers_log ON activity_attempt_answers (attempt_log_id);

CREATE TABLE IF NOT EXISTS activity_attempt_answer_options (
    answer_id UUID NOT NULL REFERENCES activity_attempt_answers (id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES activity_checklist_options (id),
    PRIMARY KEY (answer_id, option_id)
);

--rollback DROP TABLE activity_attempt_answer_options; DROP TABLE activity_attempt_answers; DROP TABLE activity_attempt_logs; DROP TABLE activity_assignments;
