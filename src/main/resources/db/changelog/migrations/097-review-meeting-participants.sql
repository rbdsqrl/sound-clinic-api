--liquibase formatted sql

--changeset simplehearing:097-review-meeting-participants
-- Review meetings move from a fully-derived attendee list (therapist + every linked parent,
-- computed at invite time) to an explicit, editable one: the patient's parents plus whichever
-- Clinic Head(s) were chosen at scheduling time. The assigned therapist is deliberately not a
-- participant under the new model — therapist_id on review_meetings is kept purely for
-- clinical/analytics attribution, not for calendar invites.
CREATE TABLE IF NOT EXISTS review_meeting_participants (
    review_meeting_id UUID NOT NULL,
    user_id           UUID NOT NULL,

    PRIMARY KEY (review_meeting_id, user_id),
    CONSTRAINT fk_rmp_meeting FOREIGN KEY (review_meeting_id) REFERENCES review_meetings (id) ON DELETE CASCADE,
    CONSTRAINT fk_rmp_user    FOREIGN KEY (user_id)           REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_rmp_user ON review_meeting_participants (user_id);

-- Backfill: every existing review meeting's linked parents become explicit participants. The
-- therapist is deliberately not backfilled (no longer an invitee under the new model), and no
-- Clinic Head is backfilled either — none was ever recorded historically; only meetings
-- scheduled after this migration get one, via the new picker.
INSERT INTO review_meeting_participants (review_meeting_id, user_id)
SELECT rm.id, pp.parent_id
FROM review_meetings rm
JOIN patient_parents pp ON pp.patient_id = rm.patient_id
ON CONFLICT DO NOTHING;

--rollback DROP TABLE review_meeting_participants;
