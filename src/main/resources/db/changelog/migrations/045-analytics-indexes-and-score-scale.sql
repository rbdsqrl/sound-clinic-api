--liquibase formatted sql

--changeset simplehearing:045-analytics-score-scale
-- performance_score was added in 033 as an unbounded INTEGER with no rubric, so any
-- value already recorded predates the 1-5 scale. Clamp it into range before the
-- constraint goes on, otherwise the migration fails on existing dev/prod data.
UPDATE therapy_sessions SET performance_score = 5 WHERE performance_score > 5;
UPDATE therapy_sessions SET performance_score = 1 WHERE performance_score < 1;

ALTER TABLE therapy_sessions
    ADD CONSTRAINT chk_therapy_sessions_performance_score
    CHECK (performance_score IS NULL OR performance_score BETWEEN 1 AND 5);

--rollback ALTER TABLE therapy_sessions DROP CONSTRAINT chk_therapy_sessions_performance_score;

--changeset simplehearing:045-analytics-indexes
-- Analytics reads whole date windows; none of these tables had an index supporting
-- a range scan (therapy_sessions had none at all since 024).
CREATE INDEX IF NOT EXISTS idx_sessions_org_date       ON therapy_sessions(org_id, session_date);
CREATE INDEX IF NOT EXISTS idx_sessions_patient_date   ON therapy_sessions(patient_id, session_date);
CREATE INDEX IF NOT EXISTS idx_sessions_therapist_date ON therapy_sessions(therapist_id, session_date);
CREATE INDEX IF NOT EXISTS idx_iep_progress_org_date   ON iep_goal_progress(org_id, session_date);
CREATE INDEX IF NOT EXISTS idx_review_meetings_org_date ON review_meetings(org_id, meeting_date);

--rollback DROP INDEX idx_sessions_org_date;
--rollback DROP INDEX idx_sessions_patient_date;
--rollback DROP INDEX idx_sessions_therapist_date;
--rollback DROP INDEX idx_iep_progress_org_date;
--rollback DROP INDEX idx_review_meetings_org_date;
