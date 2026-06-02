--liquibase formatted sql

--changeset simplehearing:038-create-iep
CREATE TABLE IF NOT EXISTS iep_plans (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id        UUID         NOT NULL,
    patient_id    UUID         NOT NULL,
    therapist_id  UUID         NOT NULL,
    title         VARCHAR(255) NOT NULL,
    start_date    DATE,
    end_date      DATE,
    status        VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    tags          TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS iep_goals (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id                UUID         NOT NULL,
    plan_id               UUID         NOT NULL REFERENCES iep_plans(id) ON DELETE CASCADE,
    title                 VARCHAR(255) NOT NULL,
    goal_statement        TEXT,
    domain                VARCHAR(100) NOT NULL,
    baseline              TEXT,
    target_criteria       VARCHAR(500),
    target_date           DATE,
    status                VARCHAR(50)  NOT NULL DEFAULT 'IN_PROGRESS',
    assigned_therapist_id UUID,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS iep_goal_progress (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id        UUID        NOT NULL,
    goal_id       UUID        NOT NULL REFERENCES iep_goals(id) ON DELETE CASCADE,
    therapist_id  UUID        NOT NULL,
    session_date  DATE        NOT NULL,
    note          TEXT,
    trials_passed INTEGER,
    trials_total  INTEGER,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_iep_plans_org_patient ON iep_plans(org_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_iep_goals_plan        ON iep_goals(plan_id);
CREATE INDEX IF NOT EXISTS idx_iep_progress_goal     ON iep_goal_progress(goal_id);

--rollback DROP TABLE IF EXISTS iep_goal_progress; DROP TABLE IF EXISTS iep_goals; DROP TABLE IF EXISTS iep_plans;
