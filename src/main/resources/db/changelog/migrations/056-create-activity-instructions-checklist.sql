--liquibase formatted sql

--changeset simplehearing:056-create-activity-instructions-checklist
CREATE TABLE IF NOT EXISTS activity_instructions (
    id          UUID NOT NULL PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL DEFAULT 0,
    text        TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_activity_instructions_activity ON activity_instructions (activity_id);

CREATE TABLE IF NOT EXISTS activity_checklist_questions (
    id             UUID NOT NULL PRIMARY KEY,
    activity_id    UUID NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    order_index    INTEGER NOT NULL DEFAULT 0,
    question_text  TEXT NOT NULL,
    question_type  VARCHAR(20) NOT NULL DEFAULT 'SINGLE_CHOICE'
);
CREATE INDEX IF NOT EXISTS idx_activity_checklist_questions_activity ON activity_checklist_questions (activity_id);

CREATE TABLE IF NOT EXISTS activity_checklist_options (
    id           UUID NOT NULL PRIMARY KEY,
    question_id  UUID NOT NULL REFERENCES activity_checklist_questions (id) ON DELETE CASCADE,
    order_index  INTEGER NOT NULL DEFAULT 0,
    option_text  VARCHAR(500) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_activity_checklist_options_question ON activity_checklist_options (question_id);

--rollback DROP TABLE activity_checklist_options; DROP TABLE activity_checklist_questions; DROP TABLE activity_instructions;
