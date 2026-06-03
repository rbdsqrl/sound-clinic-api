--liquibase formatted sql

--changeset simplehearing:040-create-iep-templates
CREATE TABLE iep_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    tags        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE iep_template_goals (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id      UUID NOT NULL REFERENCES iep_templates(id) ON DELETE CASCADE,
    org_id           UUID NOT NULL,
    title            VARCHAR(255) NOT NULL,
    goal_statement   TEXT,
    domain           VARCHAR(50),
    baseline         TEXT,
    target_criteria  TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_iep_templates_org ON iep_templates(org_id);
CREATE INDEX idx_iep_template_goals_template ON iep_template_goals(template_id);

--rollback DROP TABLE iep_template_goals; DROP TABLE iep_templates;
