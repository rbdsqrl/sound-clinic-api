--liquibase formatted sql

--changeset simplehearing:127-shared-media-read-receipts
-- When the intended recipient (the parent for a clinic-to-family post, an assigned therapist for a
-- family-to-clinic post) first opened it, and who. NULL = not read yet.
ALTER TABLE shared_media ADD COLUMN IF NOT EXISTS read_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE shared_media ADD COLUMN IF NOT EXISTS read_by UUID;

--rollback ALTER TABLE shared_media DROP COLUMN IF EXISTS read_by; ALTER TABLE shared_media DROP COLUMN IF EXISTS read_at;
