--liquibase formatted sql

--changeset simplehearing:070-create-feed-posts
-- Clinic-wide announcement posts. Readable by every role in the org; only
-- BUSINESS_OWNER/CLINIC_HEAD can create, edit, or delete one.
CREATE TABLE IF NOT EXISTS feed_posts (
    id         UUID         PRIMARY KEY,
    org_id     UUID         NOT NULL,
    author_id  UUID         NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feed_posts_org_created ON feed_posts (org_id, created_at DESC);

--rollback DROP TABLE feed_posts;
