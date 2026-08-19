--liquibase formatted sql

--changeset simplehearing:044-alter-enrollments-add-end-date
-- Until now a therapy plan's end was only implied by its session count. Review
-- meetings need an explicit window to be scheduled across, so store it.
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS end_date DATE;

--rollback ALTER TABLE enrollments DROP COLUMN end_date;

--changeset simplehearing:044-create-review-meetings
CREATE TABLE IF NOT EXISTS review_meetings (
    id                        UUID         PRIMARY KEY,
    org_id                    UUID         NOT NULL,
    enrollment_id             UUID         NOT NULL,
    patient_id                UUID         NOT NULL,
    therapist_id              UUID         NOT NULL,
    meeting_number            INTEGER      NOT NULL,
    meeting_date              DATE         NOT NULL,
    start_time                TIME         NOT NULL,
    end_time                  TIME         NOT NULL,
    status                    VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',

    -- Parent's feedback about the therapist
    parent_rating             INTEGER,
    parent_comments           TEXT,
    parent_feedback_by        UUID,
    parent_feedback_at        TIMESTAMP WITH TIME ZONE,

    -- Therapist's feedback about the period under review
    therapist_summary         TEXT,
    therapist_progress_notes  TEXT,
    therapist_feedback_at     TIMESTAMP WITH TIME ZONE,

    -- Bumped on every reschedule so calendar clients update rather than duplicate
    ics_sequence              INTEGER      NOT NULL DEFAULT 0,
    ics_uid                   VARCHAR(255) NOT NULL,

    cancelled_reason          TEXT,
    created_by                UUID,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_review_meetings_ics_uid       UNIQUE (ics_uid),
    CONSTRAINT ck_review_meetings_parent_rating CHECK (parent_rating IS NULL OR parent_rating BETWEEN 1 AND 5),
    CONSTRAINT fk_review_meetings_enrollment    FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_meetings_patient       FOREIGN KEY (patient_id)    REFERENCES patients (id)    ON DELETE CASCADE,
    CONSTRAINT fk_review_meetings_therapist     FOREIGN KEY (therapist_id)  REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_review_meetings_enrollment ON review_meetings (enrollment_id);
CREATE INDEX IF NOT EXISTS idx_review_meetings_patient    ON review_meetings (patient_id);
CREATE INDEX IF NOT EXISTS idx_review_meetings_therapist  ON review_meetings (therapist_id);
CREATE INDEX IF NOT EXISTS idx_review_meetings_date       ON review_meetings (org_id, meeting_date);

--rollback DROP TABLE review_meetings;
