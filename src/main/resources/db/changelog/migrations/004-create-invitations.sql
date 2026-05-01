--liquibase formatted sql

--changeset simplehearing:004-create-invitations
CREATE TABLE invitations (
    id          UUID         PRIMARY KEY,
    org_id      UUID         NOT NULL,
    clinic_id   UUID,
    invited_by  UUID         NOT NULL,
    email       VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_invitations_org        FOREIGN KEY (org_id)     REFERENCES organisations (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_clinic     FOREIGN KEY (clinic_id)  REFERENCES clinics (id),
    CONSTRAINT fk_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES users (id)
);

CREATE INDEX idx_invitations_org_id     ON invitations (org_id);
CREATE INDEX idx_invitations_token_hash ON invitations (token_hash);
CREATE INDEX idx_invitations_email      ON invitations (email);

--rollback DROP TABLE invitations;
