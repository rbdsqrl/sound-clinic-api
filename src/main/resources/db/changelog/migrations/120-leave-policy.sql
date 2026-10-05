--liquibase formatted sql

--changeset simplehearing:120-leave-policy
-- Leave categories (e.g. Casual, Sick) with a yearly quota, optional per-person allocations, and the
-- month the organisation's leave year starts. An organisation with no categories behaves exactly as
-- before: leave is requested and approved with no quota.
CREATE TABLE IF NOT EXISTS leave_categories (
    id           UUID          NOT NULL PRIMARY KEY,
    org_id       UUID          NOT NULL,
    name         VARCHAR(80)   NOT NULL,
    annual_days  INT,                                   -- NULL = no yearly limit
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_leave_categories_org_name ON leave_categories (org_id, lower(name));

-- A person's own allocation for one category in one leave year, overriding the category's default.
CREATE TABLE IF NOT EXISTS leave_allocations (
    id           UUID          NOT NULL PRIMARY KEY,
    org_id       UUID          NOT NULL,
    user_id      UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id  UUID          NOT NULL REFERENCES leave_categories (id) ON DELETE CASCADE,
    leave_year   INT           NOT NULL,                -- the calendar year the leave year STARTS in
    days         INT           NOT NULL,
    CONSTRAINT uq_leave_allocation UNIQUE (user_id, category_id, leave_year)
);

ALTER TABLE leaves ADD COLUMN IF NOT EXISTS category_id UUID REFERENCES leave_categories (id) ON DELETE SET NULL;
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS leave_year_start_month INT NOT NULL DEFAULT 1;

--rollback ALTER TABLE organisations DROP COLUMN IF EXISTS leave_year_start_month; ALTER TABLE leaves DROP COLUMN IF EXISTS category_id; DROP TABLE IF EXISTS leave_allocations; DROP TABLE IF EXISTS leave_categories;
