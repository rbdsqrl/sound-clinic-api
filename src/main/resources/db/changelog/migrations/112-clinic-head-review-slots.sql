--liquibase formatted sql

--changeset simplehearing:112-clinic-head-review-slots

-- A CLINIC_HEAD's own Review Session slot grid, overriding the org-wide default
-- (organisation_review_slot_times, see 111) — no rows for a user means "use the org default".
-- Deliberately no backfill: every existing Clinic Head starts on the org default unchanged.
CREATE TABLE IF NOT EXISTS user_review_slot_times (
    user_id   UUID NOT NULL REFERENCES users (id),
    slot_time TIME NOT NULL,
    PRIMARY KEY (user_id, slot_time)
);

--rollback DROP TABLE IF EXISTS user_review_slot_times;
