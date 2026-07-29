--liquibase formatted sql

--changeset simplehearing:042-attendance-face-override
ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS face_override              BOOLEAN                  NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS override_approved          BOOLEAN,
    ADD COLUMN IF NOT EXISTS override_reviewed_by       UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS override_reviewed_at       TIMESTAMP WITH TIME ZONE;

--rollback ALTER TABLE attendance DROP COLUMN IF EXISTS face_override, DROP COLUMN IF EXISTS override_approved, DROP COLUMN IF EXISTS override_reviewed_by, DROP COLUMN IF EXISTS override_reviewed_at;
