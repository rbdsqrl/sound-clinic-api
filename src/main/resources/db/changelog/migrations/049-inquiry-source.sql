--liquibase formatted sql

--changeset simplehearing:049-inquiry-source
-- Until now every inquiry arrived through the public website form, so existing rows
-- are WEBSITE by fact rather than by assumption. Staff-entered inquiries (walk-ins
-- and phone calls) can now be told apart in the list and counted separately.
ALTER TABLE inquiries ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'WEBSITE';

CREATE INDEX IF NOT EXISTS idx_inquiries_org_source ON inquiries (org_id, source);

--rollback DROP INDEX IF EXISTS idx_inquiries_org_source;
--rollback ALTER TABLE inquiries DROP COLUMN source;
