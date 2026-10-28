--liquibase formatted sql

--changeset simplehearing:131-renewal-outcome
-- What an admin decided when a program reached its last week (COMPLETED, DISCONTINUED or NEEDS_RENEWAL). Once set,
-- the program leaves the Renewals task list; renewing the program clears it so the next end-of-program shows up again.
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS renewal_outcome VARCHAR(30);
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS renewal_outcome_at TIMESTAMP;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS renewal_outcome_by UUID;

--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS renewal_outcome_by;
--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS renewal_outcome_at;
--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS renewal_outcome;
