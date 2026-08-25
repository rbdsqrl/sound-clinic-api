--liquibase formatted sql

--changeset simplehearing:075-programs-add-tax
-- Lets a program's price be marked tax-inclusive or tax-exclusive against one of the
-- org's Taxes (Manage → Taxes). ON DELETE SET NULL so removing a tax rate later doesn't
-- block it or orphan the program.
ALTER TABLE programs ADD COLUMN IF NOT EXISTS tax_id UUID REFERENCES taxes (id) ON DELETE SET NULL;
ALTER TABLE programs ADD COLUMN IF NOT EXISTS price_includes_tax BOOLEAN NOT NULL DEFAULT TRUE;

--rollback ALTER TABLE programs DROP COLUMN IF EXISTS price_includes_tax; ALTER TABLE programs DROP COLUMN IF EXISTS tax_id;
