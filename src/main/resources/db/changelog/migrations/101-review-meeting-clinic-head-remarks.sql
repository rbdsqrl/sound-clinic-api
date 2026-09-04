--liquibase formatted sql

--changeset simplehearing:101-review-meeting-clinic-head-remarks
-- "Therapist review" is replaced by "Clinic Head Remarks" — an Admin-only note (Clinic Head,
-- also editable by Business Owner) that a Therapist or Parent never sees, even one who also
-- holds an Admin role but is the treating therapist on this particular meeting. Fold any
-- existing progress-notes content into the summary before dropping that column, so nothing
-- already written is silently lost.
UPDATE review_meetings
SET therapist_summary = COALESCE(therapist_summary, '') ||
    CASE WHEN therapist_progress_notes IS NOT NULL AND therapist_progress_notes <> ''
         THEN (CASE WHEN therapist_summary IS NOT NULL AND therapist_summary <> '' THEN E'\n\n' ELSE '' END) || therapist_progress_notes
         ELSE '' END
WHERE therapist_progress_notes IS NOT NULL AND therapist_progress_notes <> '';

ALTER TABLE review_meetings DROP COLUMN IF EXISTS therapist_progress_notes;
ALTER TABLE review_meetings RENAME COLUMN therapist_summary TO clinic_head_remarks;
ALTER TABLE review_meetings RENAME COLUMN therapist_feedback_at TO clinic_head_remarks_at;
ALTER TABLE review_meetings ADD COLUMN IF NOT EXISTS clinic_head_remarks_by UUID REFERENCES users (id);

--rollback ALTER TABLE review_meetings DROP COLUMN IF EXISTS clinic_head_remarks_by;
--rollback ALTER TABLE review_meetings RENAME COLUMN clinic_head_remarks_at TO therapist_feedback_at;
--rollback ALTER TABLE review_meetings RENAME COLUMN clinic_head_remarks TO therapist_summary;
--rollback ALTER TABLE review_meetings ADD COLUMN IF NOT EXISTS therapist_progress_notes TEXT;
