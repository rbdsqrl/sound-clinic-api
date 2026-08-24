--liquibase formatted sql

--changeset simplehearing:054-create-activities
CREATE TABLE IF NOT EXISTS activities (
    id                    UUID          PRIMARY KEY,
    org_id                UUID          NOT NULL,
    title                 VARCHAR(255)  NOT NULL,
    about_activity        TEXT          NOT NULL,
    therapy_id            UUID,
    duration_weeks        INTEGER       NOT NULL,
    age_min_value         INTEGER       NOT NULL,
    age_min_unit          VARCHAR(10)   NOT NULL DEFAULT 'YEAR',
    age_max_value         INTEGER       NOT NULL,
    age_max_unit          VARCHAR(10)   NOT NULL DEFAULT 'YEAR',
    difficulty            VARCHAR(10)   NOT NULL DEFAULT 'EASY',
    tips_and_suggestions  TEXT,
    is_shared             BOOLEAN       NOT NULL DEFAULT false,
    source_activity_id    UUID,
    is_active             BOOLEAN       NOT NULL DEFAULT true,
    created_by             UUID,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_activities_therapy FOREIGN KEY (therapy_id) REFERENCES therapies (id),
    CONSTRAINT fk_activities_source  FOREIGN KEY (source_activity_id) REFERENCES activities (id)
);

CREATE INDEX IF NOT EXISTS idx_activities_org_id    ON activities (org_id);
CREATE INDEX IF NOT EXISTS idx_activities_therapy   ON activities (therapy_id);
CREATE INDEX IF NOT EXISTS idx_activities_shared    ON activities (is_shared) WHERE is_shared = true;

--rollback DROP TABLE activities;
