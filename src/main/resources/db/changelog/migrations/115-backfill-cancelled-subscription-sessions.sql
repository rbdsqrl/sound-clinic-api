--liquibase formatted sql

--changeset simplehearing:115-backfill-cancelled-subscription-sessions
-- Cancelling a subscription never cascaded to its enrollment/sessions/review meetings (fixed in
-- SubscriptionController#cancel / EnrollmentCancellationService going forward) — this backfills
-- the orphaned state left by every subscription already cancelled before that fix: the
-- enrollment stayed ACTIVE and its still-ahead sessions stayed SCHEDULED (or
-- PENDING_RESCHEDULE/CANCELLATION_REQUESTED), fully live on the calendar, with no sign the plan
-- behind them was cancelled. Mirrors the same "still ahead" (today or later) scope the live
-- cascade uses. Past sessions are left untouched — they're history, not something to retcon.

UPDATE therapy_sessions
   SET status = 'CANCELLED'
 WHERE status IN ('SCHEDULED', 'PENDING_RESCHEDULE', 'CANCELLATION_REQUESTED')
   AND session_date >= CURRENT_DATE
   AND enrollment_id IN (
       SELECT id FROM enrollments
        WHERE subscription_id IN (SELECT id FROM subscriptions WHERE status = 'CANCELLED')
   );

UPDATE review_meetings
   SET status = 'CANCELLED',
       cancelled_reason = 'Program cancelled'
 WHERE status = 'SCHEDULED'
   AND meeting_date >= CURRENT_DATE
   AND enrollment_id IN (
       SELECT id FROM enrollments
        WHERE subscription_id IN (SELECT id FROM subscriptions WHERE status = 'CANCELLED')
   );

UPDATE enrollments
   SET status = 'CANCELLED'
 WHERE status = 'ACTIVE'
   AND subscription_id IN (SELECT id FROM subscriptions WHERE status = 'CANCELLED');

--rollback SELECT 1;
