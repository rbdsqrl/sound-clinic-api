--liquibase formatted sql

--changeset simplehearing:010-create-patient-conditions
CREATE TABLE patient_conditions (
    patient_id    UUID NOT NULL,
    condition_id  UUID NOT NULL,
    diagnosed_at  DATE,
    notes         TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_patient_conditions  PRIMARY KEY (patient_id, condition_id),
    CONSTRAINT fk_pc_patient          FOREIGN KEY (patient_id)   REFERENCES patients (id)   ON DELETE CASCADE,
    CONSTRAINT fk_pc_condition        FOREIGN KEY (condition_id) REFERENCES conditions (id)
);

--rollback DROP TABLE patient_conditions;
