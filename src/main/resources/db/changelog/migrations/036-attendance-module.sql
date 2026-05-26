--liquibase formatted sql

--changeset simplehearing:036-attendance-module

-- Geo-fence fields on clinics
ALTER TABLE clinics ADD COLUMN IF NOT EXISTS latitude FLOAT8;
ALTER TABLE clinics ADD COLUMN IF NOT EXISTS longitude FLOAT8;
ALTER TABLE clinics ADD COLUMN IF NOT EXISTS geo_fence_radius_meters INT DEFAULT 200;

-- Face descriptor on users (128-float JSON array from face-api.js)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS face_descriptor TEXT;

-- Attendance records
CREATE TABLE IF NOT EXISTS attendance (
    id                UUID                     PRIMARY KEY,
    org_id            UUID                     NOT NULL,
    user_id           UUID                     NOT NULL,
    clinic_id         UUID                     NOT NULL,
    attendance_date   DATE                     NOT NULL,
    check_in_time     TIMESTAMP WITH TIME ZONE,
    check_out_time    TIMESTAMP WITH TIME ZONE,
    check_in_lat      FLOAT8,
    check_in_lon      FLOAT8,
    check_out_lat     FLOAT8,
    check_out_lon     FLOAT8,
    geo_verified      BOOLEAN                  NOT NULL DEFAULT FALSE,
    face_verified     BOOLEAN                  NOT NULL DEFAULT FALSE,
    status            VARCHAR(20)              NOT NULL DEFAULT 'CHECKED_IN',
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_attendance_org    FOREIGN KEY (org_id)    REFERENCES organisations (id),
    CONSTRAINT fk_attendance_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_attendance_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id),
    CONSTRAINT uq_attendance_user_date UNIQUE (user_id, attendance_date)
);

CREATE INDEX IF NOT EXISTS idx_attendance_org_id   ON attendance (org_id);
CREATE INDEX IF NOT EXISTS idx_attendance_user_id  ON attendance (user_id);
CREATE INDEX IF NOT EXISTS idx_attendance_date     ON attendance (attendance_date);
CREATE INDEX IF NOT EXISTS idx_attendance_status   ON attendance (status);

--rollback ALTER TABLE clinics DROP COLUMN IF EXISTS latitude, DROP COLUMN IF EXISTS longitude, DROP COLUMN IF EXISTS geo_fence_radius_meters;
--rollback ALTER TABLE users DROP COLUMN face_descriptor;
--rollback DROP TABLE attendance;
