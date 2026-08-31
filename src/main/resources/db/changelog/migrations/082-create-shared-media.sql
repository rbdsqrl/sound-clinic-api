--liquibase formatted sql

--changeset simplehearing:082-create-shared-media
CREATE TABLE IF NOT EXISTS shared_media (
    id               UUID          NOT NULL PRIMARY KEY,
    org_id           UUID          NOT NULL,
    patient_id       UUID          NOT NULL REFERENCES patients (id) ON DELETE CASCADE,
    uploaded_by      UUID          NOT NULL REFERENCES users (id),
    direction        VARCHAR(20)   NOT NULL, -- PARENT_TO_CLINIC | CLINIC_TO_PARENT
    file_name        VARCHAR(255),
    file_url         VARCHAR(1000),
    content_type     VARCHAR(100),
    file_size_bytes  BIGINT,
    note             TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_shared_media_has_content CHECK (file_url IS NOT NULL OR note IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS idx_shared_media_patient ON shared_media (patient_id);

--rollback DROP TABLE shared_media;
