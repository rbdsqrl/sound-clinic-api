--liquibase formatted sql

--changeset simplehearing:073-create-case-histories
-- One row per patient — a single clinical intake record, edited in place (not an episode log
-- like discharge_records). Checkbox-group selections, the fixed milestone-skills list, and the
-- free-form family-members list are stored as JSON text, matching this codebase's existing
-- TEXT-based convention (see 067-create-discharge-records.sql) rather than a jsonb column.
CREATE TABLE IF NOT EXISTS case_histories (
    id                          UUID         PRIMARY KEY,
    org_id                      UUID         NOT NULL,
    patient_id                  UUID         NOT NULL UNIQUE,

    -- Basic Concerns
    present_complaints          TEXT,
    habits                      TEXT,
    physical_other_problems     TEXT,

    -- Birth History
    prenatal_health              TEXT,
    delivery_type                VARCHAR(20),
    labour_type                  VARCHAR(20),
    birth_cry                    VARCHAR(20),
    prenatal_notes                TEXT,
    birth_additional_notes        TEXT,
    birth_height                  NUMERIC(6,2),
    birth_height_unit             VARCHAR(10),
    birth_weight                  NUMERIC(6,2),
    birth_weight_unit             VARCHAR(10),
    postnatal_health               TEXT,
    phototherapy_days              INTEGER,
    postnatal_notes                TEXT,

    -- Milestones
    motor_milestones              VARCHAR(20),
    speech_milestones             VARCHAR(20),
    milestone_skills              TEXT,
    milestones_additional_notes   TEXT,
    handedness                    VARCHAR(20),

    -- Family History
    family_type                   VARCHAR(20),
    family_members                TEXT,
    consanguinity_history          BOOLEAN,
    family_impairments_notes       TEXT,

    -- Social & Behavior History
    eye_contact                    VARCHAR(40),
    stuttering_frequency           VARCHAR(20),
    play_behavior                  VARCHAR(30),
    social_smiling                 VARCHAR(20),
    behavioural_self_regulation    VARCHAR(20),
    emotional_self_regulation      VARCHAR(20),
    friendships                    VARCHAR(30),
    listening                      VARCHAR(60),
    communications                 TEXT,
    behavioral_problems            TEXT,
    provisional_diagnosis          TEXT,

    -- School History
    current_grade                  VARCHAR(50),
    school                         VARCHAR(255),
    syllabus                       VARCHAR(100),
    age_of_joining                 NUMERIC(4,1),
    performance_and_progress       TEXT,
    attitude_towards_studies       TEXT,
    school_additional_notes        TEXT,

    created_by                     UUID,
    updated_by                     UUID,
    created_at                     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_case_histories_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_case_histories_org ON case_histories (org_id);

--rollback DROP TABLE case_histories;
