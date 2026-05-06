--liquibase formatted sql

--changeset simplehearing:018-add-patient-stage
ALTER TABLE patients ADD COLUMN stage VARCHAR(50) NOT NULL DEFAULT 'PRE_ASSESSMENT';

--rollback ALTER TABLE patients DROP COLUMN stage;
