--liquibase formatted sql

--changeset simplehearing:136-payment-period-from-sessions
-- Migration 135 left the payments of any case that had renewed a program without a period, because the plan's dates
-- alone couldn't say which run a payment was for. The sessions can: the original run and each renewal were created
-- as separate batches, so a plan's regular (non-extra) sessions fall into clusters by creation time, one per run.
-- A payment belongs to the latest run that existed when it was recorded (the first run if it predates all of them),
-- and gets that run's first and last session date.
WITH ordered AS (
    SELECT ts.enrollment_id, e.subscription_id, ts.session_date, ts.created_at,
           CASE WHEN lag(ts.created_at) OVER w IS NULL OR ts.created_at - lag(ts.created_at) OVER w > interval '1 minute'
                THEN 1 ELSE 0 END AS starts_run
    FROM therapy_sessions ts
    JOIN enrollments e ON e.id = ts.enrollment_id AND e.status <> 'CANCELLED'
    WHERE ts.ad_hoc = FALSE
    WINDOW w AS (PARTITION BY ts.enrollment_id ORDER BY ts.created_at)
),
numbered AS (
    SELECT *, sum(starts_run) OVER (PARTITION BY enrollment_id ORDER BY created_at) AS run_no
    FROM ordered
),
runs AS (
    SELECT enrollment_id, subscription_id, run_no,
           min(created_at) AS run_created, min(session_date) AS run_start, max(session_date) AS run_end
    FROM numbered
    GROUP BY enrollment_id, subscription_id, run_no
),
pick AS (
    SELECT DISTINCT ON (p.id) p.id AS payment_id, r.run_start, r.run_end
    FROM subscription_payments p
    JOIN runs r ON r.subscription_id = p.subscription_id
    WHERE p.period_start IS NULL
    ORDER BY p.id,
             (r.run_created <= p.created_at) DESC,
             CASE WHEN r.run_created <= p.created_at THEN r.run_created END DESC NULLS LAST,
             r.run_created ASC
)
UPDATE subscription_payments p
SET period_start = pick.run_start, period_end = pick.run_end
FROM pick
WHERE p.id = pick.payment_id;

--rollback SELECT 1;
