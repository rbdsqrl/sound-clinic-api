--liquibase formatted sql

--changeset simplehearing:019-add-inquiry-appointment
ALTER TABLE inquiries ADD COLUMN appointment_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE inquiries ADD COLUMN appointment_notes TEXT;

--rollback ALTER TABLE inquiries DROP COLUMN appointment_notes; ALTER TABLE inquiries DROP COLUMN appointment_date;
