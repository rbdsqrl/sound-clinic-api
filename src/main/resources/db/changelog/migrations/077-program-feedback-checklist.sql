--liquibase formatted sql

--changeset simplehearing:077-program-feedback-checklist
CREATE TABLE IF NOT EXISTS program_feedback_questions (
    id             UUID NOT NULL PRIMARY KEY,
    org_id         UUID NOT NULL,
    program_id     UUID NOT NULL REFERENCES programs (id) ON DELETE CASCADE,
    order_index    INTEGER NOT NULL DEFAULT 0,
    question_text  TEXT NOT NULL,
    question_type  VARCHAR(20) NOT NULL DEFAULT 'MULTI_CHOICE'
);
CREATE INDEX IF NOT EXISTS idx_program_feedback_questions_program ON program_feedback_questions (program_id);

CREATE TABLE IF NOT EXISTS program_feedback_options (
    id           UUID NOT NULL PRIMARY KEY,
    question_id  UUID NOT NULL REFERENCES program_feedback_questions (id) ON DELETE CASCADE,
    order_index  INTEGER NOT NULL DEFAULT 0,
    option_text  VARCHAR(500) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_program_feedback_options_question ON program_feedback_options (question_id);

CREATE TABLE IF NOT EXISTS session_feedback_answers (
    id           UUID NOT NULL PRIMARY KEY,
    session_id   UUID NOT NULL REFERENCES therapy_sessions (id) ON DELETE CASCADE,
    question_id  UUID NOT NULL REFERENCES program_feedback_questions (id) ON DELETE CASCADE,
    text_answer  TEXT,
    UNIQUE (session_id, question_id)
);
CREATE INDEX IF NOT EXISTS idx_session_feedback_answers_session ON session_feedback_answers (session_id);

CREATE TABLE IF NOT EXISTS session_feedback_answer_options (
    answer_id UUID NOT NULL REFERENCES session_feedback_answers (id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES program_feedback_options (id) ON DELETE CASCADE,
    PRIMARY KEY (answer_id, option_id)
);

ALTER TABLE therapy_sessions ADD COLUMN IF NOT EXISTS checklist_notes TEXT;

--rollback ALTER TABLE therapy_sessions DROP COLUMN checklist_notes; DROP TABLE session_feedback_answer_options; DROP TABLE session_feedback_answers; DROP TABLE program_feedback_options; DROP TABLE program_feedback_questions;
