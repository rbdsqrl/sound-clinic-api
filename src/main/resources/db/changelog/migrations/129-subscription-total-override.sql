--liquibase formatted sql

--changeset simplehearing:129-subscription-total-override
-- An admin-agreed total for the whole plan (e.g. an ad hoc discount given at renewal). When set it is the amount
-- due, instead of sessions x per-session cost less the discount percentage, which can't express an exact rupee figure.
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS total_override NUMERIC(12,2);

--rollback ALTER TABLE subscriptions DROP COLUMN IF EXISTS total_override;
