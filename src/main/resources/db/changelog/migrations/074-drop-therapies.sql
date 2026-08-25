--liquibase formatted sql

--changeset simplehearing:074-drop-therapies
-- Therapies (Manage → Therapies) was a standalone lookup with no consumers anywhere
-- in the app; activities already draw their "Therapy" field from Programs (see
-- 060-activities-use-programs.sql). Removing the unused table and feature.
DROP TABLE IF EXISTS therapies;

--rollback CREATE TABLE IF NOT EXISTS therapies (id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY, org_id UUID NOT NULL, name VARCHAR(255) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(), updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW());
