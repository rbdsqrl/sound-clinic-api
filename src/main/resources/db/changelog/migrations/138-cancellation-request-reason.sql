--liquibase formatted sql

--changeset simplehearing:138-cancellation-request-reason
-- A cancellation request now carries the reason it was raised for. The column is nullable and nothing is backfilled:
-- requests raised before this stay valid and simply have no reason to show.
ALTER TABLE therapy_sessions ADD COLUMN IF NOT EXISTS cancellation_request_reason VARCHAR(500);

--rollback ALTER TABLE therapy_sessions DROP COLUMN IF EXISTS cancellation_request_reason;
