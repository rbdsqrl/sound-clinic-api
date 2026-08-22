--liquibase formatted sql

--changeset simplehearing:051-adhoc-sessions
-- Sessions have always been generated in a block from an enrollment. An ad-hoc session
-- is booked by hand from the calendar — a catch-up, an extra visit, a slot squeezed in.
-- It still belongs to a plan (enrollment_id is NOT NULL), but whether it consumes one of
-- the sessions the family paid for is decided per booking.
ALTER TABLE therapy_sessions
    ADD COLUMN IF NOT EXISTS ad_hoc BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE therapy_sessions
    ADD COLUMN IF NOT EXISTS counts_toward_plan BOOLEAN NOT NULL DEFAULT true;

--rollback ALTER TABLE therapy_sessions DROP COLUMN counts_toward_plan;
--rollback ALTER TABLE therapy_sessions DROP COLUMN ad_hoc;
