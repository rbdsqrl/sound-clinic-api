--liquibase formatted sql

--changeset simplehearing:037-programs-add-description
ALTER TABLE programs ADD COLUMN IF NOT EXISTS description TEXT;

--rollback ALTER TABLE programs DROP COLUMN description;

--changeset simplehearing:038-conditions-add-org
ALTER TABLE conditions ADD COLUMN IF NOT EXISTS org_id UUID;
ALTER TABLE conditions DROP CONSTRAINT IF EXISTS conditions_name_key;

--rollback ALTER TABLE conditions DROP COLUMN org_id;

--changeset simplehearing:039-create-therapies
CREATE TABLE IF NOT EXISTS therapies (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id      UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

--rollback DROP TABLE IF EXISTS therapies;

--changeset simplehearing:040-create-taxes
CREATE TABLE IF NOT EXISTS taxes (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id      UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    rate        FLOAT8       NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

--rollback DROP TABLE IF EXISTS taxes;
