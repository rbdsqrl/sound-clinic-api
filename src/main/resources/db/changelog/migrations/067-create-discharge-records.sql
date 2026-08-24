--liquibase formatted sql

--changeset simplehearing:067-create-discharge-records
-- One row per discharge episode. Snapshots are frozen at discharge time (JSON text, matching
-- this codebase's existing TEXT-based convention — no jsonb type has been used elsewhere) so
-- later data changes never retroactively alter a report that was already handed to a family.
CREATE TABLE IF NOT EXISTS discharge_records (
    id                          UUID         PRIMARY KEY,
    org_id                      UUID         NOT NULL,
    patient_id                  UUID         NOT NULL,
    discharge_date              DATE         NOT NULL,
    discharged_by               UUID         NOT NULL,
    episode_start_date          DATE,
    final_assessment_snapshot   TEXT,
    goals_at_discharge_snapshot TEXT,
    avg_communication_rating    NUMERIC(4,2),
    avg_progress_rating_pct     NUMERIC(5,2),
    goal_mastery_pct            NUMERIC(5,2),
    goal_mastery_met            BOOLEAN,
    therapist_signoff_met       BOOLEAN      NOT NULL DEFAULT FALSE,
    parent_satisfaction_met     BOOLEAN,
    overall_successful          BOOLEAN      NOT NULL DEFAULT FALSE,
    notes                       TEXT,
    pdf_url                     VARCHAR(1000),
    pdf_generated_at            TIMESTAMP WITH TIME ZONE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_discharge_records_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_discharge_records_org     ON discharge_records (org_id);
CREATE INDEX IF NOT EXISTS idx_discharge_records_patient ON discharge_records (patient_id);

--rollback DROP TABLE discharge_records;
