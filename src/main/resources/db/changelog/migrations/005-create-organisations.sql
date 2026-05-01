--liquibase formatted sql

--changeset simplehearing:005-create-organisations
CREATE TABLE organisations (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(30),
    address       TEXT,
    logo_url      VARCHAR(500),
    timezone      VARCHAR(60)  NOT NULL DEFAULT 'UTC',
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_organisations_slug UNIQUE (slug)
);

--rollback DROP TABLE organisations;
