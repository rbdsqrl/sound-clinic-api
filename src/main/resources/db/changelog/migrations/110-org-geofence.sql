--liquibase formatted sql

--changeset simplehearing:110-org-geofence

-- Geo-fence fields on organisations — mirrors clinics (036-attendance-module.sql).
-- Used for BUSINESS_OWNER attendance check-in, which is verified against the org's
-- registered address rather than any single clinic.
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS latitude FLOAT8;
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS longitude FLOAT8;
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS geo_fence_radius_meters INT DEFAULT 200;

--rollback ALTER TABLE organisations DROP COLUMN IF EXISTS latitude, DROP COLUMN IF EXISTS longitude, DROP COLUMN IF EXISTS geo_fence_radius_meters;
