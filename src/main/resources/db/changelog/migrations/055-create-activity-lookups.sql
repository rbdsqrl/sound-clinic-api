--liquibase formatted sql

--changeset simplehearing:055-create-activity-lookups
CREATE TABLE IF NOT EXISTS skills (
    id         UUID         PRIMARY KEY,
    org_id     UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_skills_org_id ON skills (org_id);

CREATE TABLE IF NOT EXISTS languages (
    id         UUID         PRIMARY KEY,
    org_id     UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_languages_org_id ON languages (org_id);

CREATE TABLE IF NOT EXISTS props (
    id         UUID         PRIMARY KEY,
    org_id     UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_props_org_id ON props (org_id);

CREATE TABLE IF NOT EXISTS activity_skills (
    activity_id UUID NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    skill_id    UUID NOT NULL REFERENCES skills (id) ON DELETE CASCADE,
    PRIMARY KEY (activity_id, skill_id)
);
CREATE INDEX IF NOT EXISTS idx_activity_skills_activity ON activity_skills (activity_id);

CREATE TABLE IF NOT EXISTS activity_languages (
    activity_id UUID NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    language_id UUID NOT NULL REFERENCES languages (id) ON DELETE CASCADE,
    PRIMARY KEY (activity_id, language_id)
);
CREATE INDEX IF NOT EXISTS idx_activity_languages_activity ON activity_languages (activity_id);

CREATE TABLE IF NOT EXISTS activity_props (
    activity_id UUID NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    prop_id     UUID NOT NULL REFERENCES props (id) ON DELETE CASCADE,
    PRIMARY KEY (activity_id, prop_id)
);
CREATE INDEX IF NOT EXISTS idx_activity_props_activity ON activity_props (activity_id);

--rollback DROP TABLE activity_props; DROP TABLE activity_languages; DROP TABLE activity_skills; DROP TABLE props; DROP TABLE languages; DROP TABLE skills;
