--liquibase formatted sql

--changeset simplehearing:114-enrollment-awaiting-payment
-- Payment is no longer a precondition for enrolling — sessions are generated and scheduled
-- immediately, held "awaiting payment" (visible everywhere, just locked/badged) until the
-- linked subscription's paymentStatus reaches PAID. Mirrors the existing session-level
-- boolean-marker pattern (ad_hoc, counts_toward_plan, requires_payment,
-- cancelled_by_program_completion) rather than a new TherapySessionStatus value.
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS awaiting_payment BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE therapy_sessions ADD COLUMN IF NOT EXISTS awaiting_payment BOOLEAN NOT NULL DEFAULT FALSE;

--rollback ALTER TABLE enrollments DROP COLUMN IF EXISTS awaiting_payment;
--rollback ALTER TABLE therapy_sessions DROP COLUMN IF EXISTS awaiting_payment;

--changeset simplehearing:114-subscription-payment-reminder-sent-at
-- Drives PaymentReminderJob's every-2-days cadence. NULL = never reminded yet.
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS payment_reminder_sent_at TIMESTAMPTZ;

--rollback ALTER TABLE subscriptions DROP COLUMN IF EXISTS payment_reminder_sent_at;
