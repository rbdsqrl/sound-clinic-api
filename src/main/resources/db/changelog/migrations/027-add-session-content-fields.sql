--liquibase formatted sql

--changeset simplehearing:027-add-session-content-fields
ALTER TABLE therapy_sessions ADD COLUMN feedback TEXT;
ALTER TABLE therapy_sessions ADD COLUMN progress_report TEXT;

--rollback ALTER TABLE therapy_sessions DROP COLUMN feedback;
--rollback ALTER TABLE therapy_sessions DROP COLUMN progress_report;
