--liquibase formatted sql

--changeset simplehearing:048-performance-score-percentage
-- The 1-5 rubric added in 045 becomes a 0-100 percentage. A rubric pick and a
-- percentage are not the same judgement, so existing scores are cleared rather
-- than rescaled — a therapist who chose "3 · On Track" did not mean "60%", and
-- inventing that number would put fabricated figures into the analytics trends.
-- The sessions themselves, their notes and feedback are untouched.
ALTER TABLE therapy_sessions DROP CONSTRAINT IF EXISTS chk_therapy_sessions_performance_score;

UPDATE therapy_sessions SET performance_score = NULL WHERE performance_score IS NOT NULL;

ALTER TABLE therapy_sessions
    ADD CONSTRAINT chk_therapy_sessions_performance_score
    CHECK (performance_score IS NULL OR performance_score BETWEEN 0 AND 100);

--rollback ALTER TABLE therapy_sessions DROP CONSTRAINT chk_therapy_sessions_performance_score;
--rollback ALTER TABLE therapy_sessions ADD CONSTRAINT chk_therapy_sessions_performance_score CHECK (performance_score IS NULL OR performance_score BETWEEN 1 AND 5);
