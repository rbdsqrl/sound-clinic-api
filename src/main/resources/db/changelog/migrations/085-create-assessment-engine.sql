--liquibase formatted sql

--changeset simplehearing:085-create-assessment-engine
CREATE TABLE IF NOT EXISTS assessment_definitions (
    id               UUID NOT NULL PRIMARY KEY,
    code             VARCHAR(30) NOT NULL UNIQUE,
    name             VARCHAR(120) NOT NULL,
    description      TEXT,
    scoring_type     VARCHAR(20) NOT NULL,
    display_order    INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assessment_categories (
    id               UUID NOT NULL PRIMARY KEY,
    definition_id    UUID NOT NULL REFERENCES assessment_definitions (id) ON DELETE CASCADE,
    name             VARCHAR(120) NOT NULL,
    display_order    INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_assessment_categories_definition ON assessment_categories (definition_id, display_order);

CREATE TABLE IF NOT EXISTS assessment_items (
    id               UUID NOT NULL PRIMARY KEY,
    category_id      UUID NOT NULL REFERENCES assessment_categories (id) ON DELETE CASCADE,
    item_number      INTEGER NOT NULL,
    text             TEXT NOT NULL,
    item_type        VARCHAR(20) NOT NULL,
    display_order    INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_assessment_items_category ON assessment_items (category_id, display_order);

CREATE TABLE IF NOT EXISTS assessment_item_options (
    id               UUID NOT NULL PRIMARY KEY,
    item_id          UUID NOT NULL REFERENCES assessment_items (id) ON DELETE CASCADE,
    label            VARCHAR(200) NOT NULL,
    score            INTEGER,
    display_order    INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_assessment_item_options_item ON assessment_item_options (item_id, display_order);

CREATE TABLE IF NOT EXISTS assessment_classification_bands (
    id               UUID NOT NULL PRIMARY KEY,
    definition_id    UUID NOT NULL REFERENCES assessment_definitions (id) ON DELETE CASCADE,
    min_age_years    NUMERIC,
    max_age_years    NUMERIC,
    min_score        INTEGER,
    max_score        INTEGER,
    label            VARCHAR(60) NOT NULL,
    display_order    INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_assessment_classification_bands_definition ON assessment_classification_bands (definition_id, display_order);

-- patient_assessments: widen for the new engine. assessment_type/item_scores stay for now
-- (dropped in 089 once the legacy ISAA/PRBA rows are backfilled onto definition_id).
ALTER TABLE patient_assessments ADD COLUMN IF NOT EXISTS definition_id UUID REFERENCES assessment_definitions (id);
ALTER TABLE patient_assessments ALTER COLUMN classification TYPE VARCHAR(60);
ALTER TABLE patient_assessments ALTER COLUMN assessment_type DROP NOT NULL;
ALTER TABLE patient_assessments ALTER COLUMN item_scores DROP NOT NULL;
ALTER TABLE patient_assessments ALTER COLUMN total_score DROP NOT NULL;
CREATE INDEX IF NOT EXISTS idx_patient_assessments_definition ON patient_assessments (patient_id, definition_id, assessment_date);

CREATE TABLE IF NOT EXISTS patient_assessment_responses (
    id                     UUID NOT NULL PRIMARY KEY,
    patient_assessment_id  UUID NOT NULL REFERENCES patient_assessments (id) ON DELETE CASCADE,
    item_id                UUID NOT NULL REFERENCES assessment_items (id),
    selected_option_id     UUID REFERENCES assessment_item_options (id),
    text_value             TEXT
);
CREATE INDEX IF NOT EXISTS idx_patient_assessment_responses_assessment ON patient_assessment_responses (patient_assessment_id);

--rollback DROP TABLE patient_assessment_responses;
--rollback ALTER TABLE patient_assessments DROP COLUMN definition_id;
--rollback DROP TABLE assessment_classification_bands;
--rollback DROP TABLE assessment_item_options;
--rollback DROP TABLE assessment_items;
--rollback DROP TABLE assessment_categories;
--rollback DROP TABLE assessment_definitions;
