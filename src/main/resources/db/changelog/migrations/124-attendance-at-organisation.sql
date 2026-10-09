--liquibase formatted sql

--changeset simplehearing:124-attendance-at-organisation
-- A Business Owner can check in at the organisation's own location instead of a clinic. Such a
-- record has no clinic, so clinic_id becomes optional and at_organisation says which kind it is.
ALTER TABLE attendance ALTER COLUMN clinic_id DROP NOT NULL;
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS at_organisation BOOLEAN NOT NULL DEFAULT FALSE;

--rollback ALTER TABLE attendance DROP COLUMN IF EXISTS at_organisation;
--rollback -- clinic_id is left nullable: rows checked in at the organisation have none, so NOT NULL cannot be restored blindly
