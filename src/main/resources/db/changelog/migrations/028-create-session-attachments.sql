--liquibase formatted sql

--changeset simplehearing:028-create-session-attachments
CREATE TABLE session_attachments (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id          UUID         NOT NULL,
    session_id      UUID         NOT NULL REFERENCES therapy_sessions(id) ON DELETE CASCADE,
    therapist_id    UUID         NOT NULL REFERENCES users(id),
    file_name       VARCHAR(255) NOT NULL,
    file_url        VARCHAR(1000) NOT NULL,
    content_type    VARCHAR(100),
    file_size_bytes BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_session_attachments_session_id ON session_attachments(session_id);

--rollback DROP TABLE session_attachments;
