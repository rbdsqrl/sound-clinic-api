--liquibase formatted sql

--changeset simplehearing:063-review-meeting-rating-axes
-- parent_rating conflated two different judgements — how the parent feels about the
-- therapist (a relationship/interaction quality), and how much progress they perceive
-- (a magnitude). Splitting into two axes: communication_rating keeps the 1-5 stars
-- (a rubric fits a relationship judgement); progress_rating_pct is 0-100, matching the
-- percentage convention already established for performance_score (048) and giving the
-- discharge success-criteria satisfaction threshold a number to compare against directly.
--
-- parent_rating is kept (not dropped) and backfilled into communication_rating, since it
-- was closer to "how do you feel about the therapist" than to a progress judgement.
-- progress_rating_pct is left NULL for historical rows — it cannot be inferred.
ALTER TABLE review_meetings ADD COLUMN IF NOT EXISTS communication_rating INTEGER;
ALTER TABLE review_meetings ADD COLUMN IF NOT EXISTS progress_rating_pct INTEGER;

UPDATE review_meetings SET communication_rating = parent_rating WHERE parent_rating IS NOT NULL;

ALTER TABLE review_meetings
    ADD CONSTRAINT chk_review_meetings_communication_rating
    CHECK (communication_rating IS NULL OR communication_rating BETWEEN 1 AND 5);

ALTER TABLE review_meetings
    ADD CONSTRAINT chk_review_meetings_progress_rating_pct
    CHECK (progress_rating_pct IS NULL OR progress_rating_pct BETWEEN 0 AND 100);

--rollback ALTER TABLE review_meetings DROP CONSTRAINT IF EXISTS chk_review_meetings_communication_rating;
--rollback ALTER TABLE review_meetings DROP CONSTRAINT IF EXISTS chk_review_meetings_progress_rating_pct;
--rollback ALTER TABLE review_meetings DROP COLUMN IF EXISTS communication_rating;
--rollback ALTER TABLE review_meetings DROP COLUMN IF EXISTS progress_rating_pct;
