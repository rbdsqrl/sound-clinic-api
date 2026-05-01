--liquibase formatted sql

--changeset simplehearing:011-create-patient-parents
CREATE TABLE patient_parents (
    patient_id UUID NOT NULL,
    parent_id  UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_patient_parents PRIMARY KEY (patient_id, parent_id),
    CONSTRAINT fk_pp_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_pp_parent  FOREIGN KEY (parent_id)  REFERENCES users (id)
);

CREATE INDEX idx_patient_parents_parent_id ON patient_parents (parent_id);

--rollback DROP TABLE patient_parents;
