--liquibase formatted sql

--changeset simplehearing:025-add-patient-id-to-invitations
ALTER TABLE invitations ADD COLUMN patient_id UUID REFERENCES patients(id);

--rollback ALTER TABLE invitations DROP COLUMN patient_id;
