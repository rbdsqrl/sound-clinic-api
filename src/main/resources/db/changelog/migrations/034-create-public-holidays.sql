--liquibase formatted sql

--changeset simplehearing:034-create-public-holidays
CREATE TABLE public_holidays (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id      UUID         NOT NULL,
    holiday_date DATE        NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_by  UUID,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (org_id, holiday_date)
);

--rollback DROP TABLE public_holidays;
