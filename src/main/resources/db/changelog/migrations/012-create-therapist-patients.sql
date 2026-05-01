--liquibase formatted sql

--changeset simplehearing:012-create-therapist-patients
CREATE TABLE therapist_patients (
    id           UUID    PRIMARY KEY,
    patient_id   UUID    NOT NULL,
    therapist_id UUID    NOT NULL,
    assigned_by  UUID    NOT NULL,
    assigned_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    is_active    BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_therapist_patient   UNIQUE (patient_id, therapist_id),
    CONSTRAINT fk_tp_patient          FOREIGN KEY (patient_id)   REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_tp_therapist        FOREIGN KEY (therapist_id) REFERENCES users (id),
    CONSTRAINT fk_tp_assigned_by      FOREIGN KEY (assigned_by)  REFERENCES users (id)
);

CREATE INDEX idx_tp_patient_id   ON therapist_patients (patient_id);
CREATE INDEX idx_tp_therapist_id ON therapist_patients (therapist_id);

--rollback DROP TABLE therapist_patients;
