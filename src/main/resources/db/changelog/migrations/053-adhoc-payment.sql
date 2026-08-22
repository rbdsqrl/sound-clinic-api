--liquibase formatted sql

--changeset simplehearing:053-adhoc-payment
-- counts_toward_plan (051) says whether a session consumes one the family already paid
-- for. That leaves the extras undecided: an extra session may be chargeable or offered
-- at no cost, and the front desk needs to record which at the time of booking.
-- Only meaningful when counts_toward_plan is false — a session drawn from the plan is
-- already paid for.
ALTER TABLE therapy_sessions
    ADD COLUMN IF NOT EXISTS requires_payment BOOLEAN NOT NULL DEFAULT false;

--rollback ALTER TABLE therapy_sessions DROP COLUMN requires_payment;
