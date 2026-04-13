--liquibase formatted sql

--changeset simplehearing:001-create-clinics
CREATE TABLE clinics (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    subdomain   VARCHAR(100) NOT NULL,
    logo_url    VARCHAR(500),
    address     TEXT,
    phone       VARCHAR(30),
    email       VARCHAR(255),
    timezone    VARCHAR(60)  NOT NULL DEFAULT 'UTC',
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_clinics_subdomain UNIQUE (subdomain)
);

--rollback DROP TABLE clinics;
