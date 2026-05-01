--liquibase formatted sql

--changeset simplehearing:015-create-appointments
CREATE TABLE appointments (
    id                UUID         PRIMARY KEY,
    org_id            UUID         NOT NULL,
    patient_id        UUID         NOT NULL,
    therapist_id      UUID         NOT NULL,
    clinic_id         UUID         NOT NULL,
    booked_by         UUID,
    appointment_date  DATE         NOT NULL,
    start_time        TIME         NOT NULL,
    end_time          TIME         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    notes             TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_appt_org       FOREIGN KEY (org_id)       REFERENCES organisations (id),
    CONSTRAINT fk_appt_patient   FOREIGN KEY (patient_id)   REFERENCES patients (id),
    CONSTRAINT fk_appt_therapist FOREIGN KEY (therapist_id) REFERENCES users (id),
    CONSTRAINT fk_appt_clinic    FOREIGN KEY (clinic_id)    REFERENCES clinics (id),
    CONSTRAINT fk_appt_bookedby  FOREIGN KEY (booked_by)    REFERENCES users (id),
    CONSTRAINT uq_appt_slot      UNIQUE (therapist_id, appointment_date, start_time)
);

CREATE INDEX idx_appt_therapist_date ON appointments (therapist_id, appointment_date);
CREATE INDEX idx_appt_patient_id     ON appointments (patient_id);
CREATE INDEX idx_appt_org_id         ON appointments (org_id);
CREATE INDEX idx_appt_booked_by      ON appointments (booked_by);

--rollback DROP TABLE appointments;
