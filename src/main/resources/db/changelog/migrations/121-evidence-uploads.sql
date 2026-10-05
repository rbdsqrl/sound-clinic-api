--liquibase formatted sql

--changeset simplehearing:121-evidence-uploads
-- A video upload in flight: the server has issued a signed URL and the browser is sending the file straight
-- to storage. The row remembers what was promised (size, type, goal/session) so "complete" can verify the
-- stored object before registering the evidence. Abandoned rows (and their objects) are swept after expiry.
CREATE TABLE IF NOT EXISTS evidence_uploads (
    id               UUID          NOT NULL PRIMARY KEY,
    org_id           UUID          NOT NULL,
    patient_id       UUID          NOT NULL REFERENCES patients (id) ON DELETE CASCADE,
    uploaded_by      UUID          NOT NULL REFERENCES users (id),
    goal_id          UUID,
    session_id       UUID,
    stored_url       VARCHAR(1000) NOT NULL,
    file_name        VARCHAR(255)  NOT NULL,
    content_type     VARCHAR(100)  NOT NULL,
    size_bytes       BIGINT        NOT NULL,
    duration_seconds INT,
    note             TEXT,
    expires_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_evidence_uploads_expiry ON evidence_uploads (expires_at);

--rollback DROP TABLE IF EXISTS evidence_uploads;
