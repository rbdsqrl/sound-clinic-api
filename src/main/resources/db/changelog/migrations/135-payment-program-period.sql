--liquibase formatted sql

--changeset simplehearing:135-payment-program-period
-- Each payment now records the program run (first to last session date) it was made for, so the payment history can
-- show it. enrollments.module_start_date marks where the current run began after a renewal (null = the original
-- start date).
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS module_start_date DATE;
ALTER TABLE subscription_payments ADD COLUMN IF NOT EXISTS period_start DATE;
ALTER TABLE subscription_payments ADD COLUMN IF NOT EXISTS period_end DATE;

-- Backfill: a payment belongs to its plan's enrollment dates — unless the case has ever renewed a program, since
-- the dates alone can't tell which run an older payment was for. Those stay blank rather than show a wrong period.
UPDATE subscription_payments p
SET period_start = e.start_date, period_end = e.end_date
FROM (
    SELECT DISTINCT ON (subscription_id) subscription_id, start_date, end_date
    FROM enrollments
    WHERE status <> 'CANCELLED'
    ORDER BY subscription_id, created_at DESC
) e
WHERE p.subscription_id = e.subscription_id
  AND p.period_start IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM subscriptions s
      JOIN audit_log_entries a ON a.subject_type = 'CASE' AND a.subject_id = s.patient_id AND a.event_type = 'PROGRAM_RENEWED'
      WHERE s.id = p.subscription_id
  );

--rollback ALTER TABLE subscription_payments DROP COLUMN IF EXISTS period_end;
--rollback ALTER TABLE subscription_payments DROP COLUMN IF EXISTS period_start;
--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS module_start_date;
