--liquibase formatted sql

--changeset simplehearing:111-review-session-slots

-- The fixed daily grid a Review Meeting must be booked into — configurable per org, same
-- pattern as organisation_weekly_off_days. Defaults to 3 morning + 3 evening times.
CREATE TABLE IF NOT EXISTS organisation_review_slot_times (
    organisation_id UUID NOT NULL REFERENCES organisations (id),
    slot_time       TIME NOT NULL,
    PRIMARY KEY (organisation_id, slot_time)
);

-- Backfill the default 6 slots for every org that already exists — a brand new org gets them
-- from Organisation's Java-side field default when it's first persisted.
INSERT INTO organisation_review_slot_times (organisation_id, slot_time)
SELECT o.id, d.slot_time
FROM organisations o
CROSS JOIN (VALUES
    ('09:00'::TIME), ('10:00'::TIME), ('11:00'::TIME),
    ('15:00'::TIME), ('16:00'::TIME), ('17:00'::TIME)
) AS d(slot_time)
ON CONFLICT DO NOTHING;

--rollback DROP TABLE IF EXISTS organisation_review_slot_times;
