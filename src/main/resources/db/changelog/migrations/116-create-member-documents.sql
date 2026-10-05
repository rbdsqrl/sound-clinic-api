--liquibase formatted sql

--changeset simplehearing:116-create-member-documents
CREATE TABLE IF NOT EXISTS member_documents (
    id               UUID          NOT NULL PRIMARY KEY,
    org_id           UUID          NOT NULL,
    member_id        UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    uploaded_by      UUID          NOT NULL REFERENCES users (id),
    category         VARCHAR(30)   NOT NULL, -- IDENTITY_PROOF | QUALIFICATION | CERTIFICATION | EMPLOYMENT_CONTRACT | OTHER
    title            VARCHAR(255)  NOT NULL,
    file_name        VARCHAR(255)  NOT NULL,
    file_url         VARCHAR(1000) NOT NULL,
    content_type     VARCHAR(100),
    file_size_bytes  BIGINT,
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_member_documents_member ON member_documents (org_id, member_id, created_at DESC);

--rollback DROP TABLE member_documents;
