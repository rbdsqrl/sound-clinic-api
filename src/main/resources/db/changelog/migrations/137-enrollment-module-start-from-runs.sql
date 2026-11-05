--liquibase formatted sql

--changeset simplehearing:137-enrollment-module-start-from-runs
-- enrollments.module_start_date marks where a program's current run began after a renewal, but it was only set from
-- the next renewal onwards. Set it for programs already renewed: a program's regular (non-extra) sessions fall into
-- runs by creation time — the original run and each renewal were created as separate batches — and the current run
-- is the last one. Payments recorded from now on then get the current run's dates, not the whole stretched range.
WITH ordered AS (
    SELECT ts.enrollment_id, ts.session_date, ts.status, ts.created_at,
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
    SELECT enrollment_id, run_no,
           coalesce(min(session_date) FILTER (WHERE status <> 'CANCELLED'), min(session_date)) AS run_start
    FROM numbered
    GROUP BY enrollment_id, run_no
),
latest AS (
    SELECT DISTINCT ON (enrollment_id) enrollment_id, run_no, run_start
    FROM runs
    ORDER BY enrollment_id, run_no DESC
)
UPDATE enrollments e
SET module_start_date = latest.run_start
FROM latest
WHERE e.id = latest.enrollment_id
  AND latest.run_no > 1
  AND e.module_start_date IS NULL;

--rollback SELECT 1;
