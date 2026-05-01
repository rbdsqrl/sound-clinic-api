--liquibase formatted sql

--changeset simplehearing:014-create-therapist-slots
CREATE TABLE therapist_slots (
    id                     UUID    PRIMARY KEY,
    org_id                 UUID    NOT NULL,
    therapist_id           UUID    NOT NULL,
    clinic_id              UUID    NOT NULL,
    day_of_week            INT     NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time             TIME    NOT NULL,
    end_time               TIME    NOT NULL,
    slot_duration_minutes  INT     NOT NULL DEFAULT 30,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_ts_org       FOREIGN KEY (org_id)       REFERENCES organisations (id),
    CONSTRAINT fk_ts_therapist FOREIGN KEY (therapist_id) REFERENCES users (id),
    CONSTRAINT fk_ts_clinic    FOREIGN KEY (clinic_id)    REFERENCES clinics (id)
);

CREATE INDEX idx_ts_therapist_id ON therapist_slots (therapist_id);
CREATE INDEX idx_ts_org_id       ON therapist_slots (org_id);

--rollback DROP TABLE therapist_slots;
