--liquibase formatted sql

--changeset simplehearing:016-create-leaves
CREATE TABLE leaves (
    id            UUID         PRIMARY KEY,
    org_id        UUID         NOT NULL,
    therapist_id  UUID         NOT NULL,
    leave_date    DATE         NOT NULL,
    leave_type    VARCHAR(20)  NOT NULL,
    reason        TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by   UUID,
    reviewed_at   TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_leave_org        FOREIGN KEY (org_id)       REFERENCES organisations (id),
    CONSTRAINT fk_leave_therapist  FOREIGN KEY (therapist_id) REFERENCES users (id),
    CONSTRAINT fk_leave_reviewer   FOREIGN KEY (reviewed_by)  REFERENCES users (id)
);

CREATE INDEX idx_leave_org_id       ON leaves (org_id);
CREATE INDEX idx_leave_therapist_id ON leaves (therapist_id);
CREATE INDEX idx_leave_status       ON leaves (status);
CREATE INDEX idx_leave_date         ON leaves (leave_date);

--rollback DROP TABLE leaves;
