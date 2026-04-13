--liquibase formatted sql

--changeset simplehearing:002-create-users
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    clinic_id     UUID         NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(30),
    date_of_birth DATE,
    gender        VARCHAR(10),
    role          VARCHAR(20)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email     UNIQUE (email),
    CONSTRAINT fk_users_clinic_id FOREIGN KEY (clinic_id) REFERENCES clinics (id)
);

CREATE INDEX idx_users_clinic_id   ON users (clinic_id);
CREATE INDEX idx_users_clinic_role ON users (clinic_id, role);

--rollback DROP TABLE users;
