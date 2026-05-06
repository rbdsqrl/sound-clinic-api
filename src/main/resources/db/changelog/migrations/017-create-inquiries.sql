--liquibase formatted sql

--changeset simplehearing:017-create-inquiries
CREATE TABLE inquiries (
    id             UUID         PRIMARY KEY,
    org_id         UUID,
    name           VARCHAR(255) NOT NULL,
    email          VARCHAR(255),
    phone          VARCHAR(50)  NOT NULL,
    reason         TEXT,
    preferred_time VARCHAR(20),
    status         VARCHAR(30)  NOT NULL DEFAULT 'NEW',
    admin_notes    TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_inquiry_org_id ON inquiries (org_id);
CREATE INDEX idx_inquiry_status ON inquiries (status);
CREATE INDEX idx_inquiry_created_at ON inquiries (created_at DESC);

--rollback DROP TABLE inquiries;
