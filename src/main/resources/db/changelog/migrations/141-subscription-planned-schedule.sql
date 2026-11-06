--liquibase formatted sql

--changeset simplehearing:141-subscription-planned-schedule
-- The weekdays and start date chosen when a program is created (before payment), so the enrollment step that follows
-- — possibly days later — starts from the same schedule and the session count / price quoted up front still holds.
-- Null on plans created before this, where the enrollment step still asks for them.
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS planned_start_date DATE;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS planned_session_days VARCHAR(80);

--rollback ALTER TABLE subscriptions DROP COLUMN IF EXISTS planned_session_days;
--rollback ALTER TABLE subscriptions DROP COLUMN IF EXISTS planned_start_date;
