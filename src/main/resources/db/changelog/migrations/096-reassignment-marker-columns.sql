--liquibase formatted sql

--changeset simplehearing:096-reassignment-marker-columns
-- Marks a row as currently owned by a specific bulk therapist reassignment, mirroring the
-- cancelled_by_program_completion precedent (migration 094) but as a nullable FK rather than a
-- boolean, since several different reassignment batches can be active at once across different
-- enrollments — each moved row must remember exactly which batch moved it, so a later revert
-- only touches rows that batch owns. Cleared back to NULL when that batch reverts.
ALTER TABLE therapy_sessions   ADD COLUMN IF NOT EXISTS reassignment_id UUID REFERENCES therapist_reassignments (id);
ALTER TABLE review_meetings    ADD COLUMN IF NOT EXISTS reassignment_id UUID REFERENCES therapist_reassignments (id);
ALTER TABLE therapist_patients ADD COLUMN IF NOT EXISTS reassignment_id UUID REFERENCES therapist_reassignments (id);
ALTER TABLE iep_plans          ADD COLUMN IF NOT EXISTS reassignment_id UUID REFERENCES therapist_reassignments (id);

CREATE INDEX IF NOT EXISTS idx_sessions_reassignment ON therapy_sessions (reassignment_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reassignment  ON review_meetings (reassignment_id);

--rollback ALTER TABLE therapy_sessions DROP COLUMN IF EXISTS reassignment_id;
--rollback ALTER TABLE review_meetings DROP COLUMN IF EXISTS reassignment_id;
--rollback ALTER TABLE therapist_patients DROP COLUMN IF EXISTS reassignment_id;
--rollback ALTER TABLE iep_plans DROP COLUMN IF EXISTS reassignment_id;
