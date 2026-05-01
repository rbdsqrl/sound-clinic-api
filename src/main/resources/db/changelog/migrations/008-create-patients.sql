--liquibase formatted sql

--changeset simplehearing:008-create-patients
CREATE TABLE patients (
    id            UUID         PRIMARY KEY,
    org_id        UUID         NOT NULL,
    clinic_id     UUID         NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender        VARCHAR(10),
    notes         TEXT,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_patients_org    FOREIGN KEY (org_id)    REFERENCES organisations (id),
    CONSTRAINT fk_patients_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id)
);

CREATE INDEX idx_patients_org_id    ON patients (org_id);
CREATE INDEX idx_patients_clinic_id ON patients (clinic_id);

--rollback DROP TABLE patients;
