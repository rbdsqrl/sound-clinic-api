--liquibase formatted sql

--changeset simplehearing:078-create-patient-assessments
CREATE TABLE IF NOT EXISTS patient_assessments (
    id               UUID NOT NULL PRIMARY KEY,
    org_id           UUID NOT NULL,
    patient_id       UUID NOT NULL REFERENCES patients (id) ON DELETE CASCADE,
    assessment_type  VARCHAR(20) NOT NULL,
    assessment_date  DATE NOT NULL,
    filled_by        UUID NOT NULL REFERENCES users (id),
    item_scores      TEXT NOT NULL,
    total_score      INTEGER NOT NULL,
    classification   VARCHAR(30),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_patient_assessments_patient ON patient_assessments (patient_id, assessment_type, assessment_date);

--rollback DROP TABLE patient_assessments;
