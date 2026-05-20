--liquibase formatted sql

--changeset simplehearing:035-session-reschedule-reason
ALTER TABLE therapy_sessions
    ADD COLUMN reschedule_reason      VARCHAR(50),
    ADD COLUMN reschedule_requested_by UUID;

--rollback ALTER TABLE therapy_sessions DROP COLUMN reschedule_reason; ALTER TABLE therapy_sessions DROP COLUMN reschedule_requested_by;
