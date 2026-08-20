--liquibase formatted sql

--changeset simplehearing:047-create-meetings
-- A general-purpose meeting, unlike review_meetings which always hang off a
-- therapy plan and a patient. Staff schedule these from the calendar for case
-- discussions, team catch-ups and anything else that needs attendees.
CREATE TABLE IF NOT EXISTS meetings (
    id                UUID         PRIMARY KEY,
    org_id            UUID         NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    meeting_date      DATE         NOT NULL,
    start_time        TIME         NOT NULL,
    end_time          TIME         NOT NULL,
    location          VARCHAR(255),
    status            VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    cancelled_reason  TEXT,

    -- Calendar invite bookkeeping, mirroring review_meetings so reschedules
    -- update the entry in place rather than creating a duplicate.
    ics_uid           VARCHAR(255),
    ics_sequence      INTEGER      NOT NULL DEFAULT 0,

    created_by        UUID         NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_meetings_org        FOREIGN KEY (org_id)     REFERENCES organisations (id) ON DELETE CASCADE,
    CONSTRAINT fk_meetings_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_meetings_org_date ON meetings (org_id, meeting_date);

--rollback DROP TABLE meetings;

--changeset simplehearing:047-create-meeting-participants
CREATE TABLE IF NOT EXISTS meeting_participants (
    meeting_id UUID NOT NULL,
    user_id    UUID NOT NULL,

    PRIMARY KEY (meeting_id, user_id),
    CONSTRAINT fk_meeting_participants_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id) ON DELETE CASCADE,
    CONSTRAINT fk_meeting_participants_user    FOREIGN KEY (user_id)    REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_meeting_participants_user ON meeting_participants (user_id);

--rollback DROP TABLE meeting_participants;
