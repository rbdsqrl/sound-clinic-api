--liquibase formatted sql

--changeset simplehearing:119-goal-video-evidence
-- Video evidence for IEP goals. Each row is either a VIDEO (a stored file) or a CANNOT_UPLOAD
-- (the therapist records why they couldn't upload one) — both link to the child and, optionally,
-- a goal and a session, so evidence can also be added ad hoc. Reasons are categorised so analytics
-- can tell tooling/network problems apart from habits.
CREATE TABLE IF NOT EXISTS goal_evidence (
    id               UUID          NOT NULL PRIMARY KEY,
    org_id           UUID          NOT NULL,
    patient_id       UUID          NOT NULL REFERENCES patients (id) ON DELETE CASCADE,
    plan_id          UUID,
    goal_id          UUID          REFERENCES iep_goals (id) ON DELETE CASCADE,
    session_id       UUID          REFERENCES therapy_sessions (id) ON DELETE SET NULL,
    enrollment_id    UUID,
    therapist_id     UUID          NOT NULL REFERENCES users (id),
    kind             VARCHAR(20)   NOT NULL, -- VIDEO | CANNOT_UPLOAD
    file_name        VARCHAR(255),
    file_url         VARCHAR(1000),
    content_type     VARCHAR(100),
    file_size_bytes  BIGINT,
    duration_seconds INT,
    reason_code      VARCHAR(40),
    reason_text      TEXT,
    note             TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_goal_evidence_shape CHECK (
        (kind = 'VIDEO' AND file_url IS NOT NULL)
        OR (kind = 'CANNOT_UPLOAD' AND reason_code IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_goal_evidence_goal      ON goal_evidence (goal_id);
CREATE INDEX IF NOT EXISTS idx_goal_evidence_patient   ON goal_evidence (patient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_goal_evidence_org_date  ON goal_evidence (org_id, created_at);

-- When a goal was completed — monthly goal-completion analytics need a date, and updated_at moves on
-- every edit. Legacy completed goals are backfilled from updated_at (the best date available).
ALTER TABLE iep_goals ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE;
UPDATE iep_goals SET completed_at = updated_at WHERE status = 'COMPLETED' AND completed_at IS NULL;

-- Org-configurable evidence rules. 0 videos required = evidence is optional.
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS evidence_videos_required INT NOT NULL DEFAULT 1;
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS evidence_max_video_mb INT NOT NULL DEFAULT 50;
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS evidence_max_video_seconds INT NOT NULL DEFAULT 120;

--rollback ALTER TABLE organisations DROP COLUMN IF EXISTS evidence_max_video_seconds; ALTER TABLE organisations DROP COLUMN IF EXISTS evidence_max_video_mb; ALTER TABLE organisations DROP COLUMN IF EXISTS evidence_videos_required; ALTER TABLE iep_goals DROP COLUMN IF EXISTS completed_at; DROP TABLE IF EXISTS goal_evidence;
